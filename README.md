# InkyUtils
Small Java 25 utility library built on top of [fastutil](https://github.com/vigna/fastutil).

## Overview

### `params`
Parses compact, human-written parameter strings (commands, config values, tags) into a tree of values, lists and maps.

- Values are separated by whitespace. `key:value` makes a map entry, `[...]` a list, `{...}` a nested map.
- Values containing whitespace, colons or brackets are quoted with `'...'`; `\` and `'` are escaped with `\`.
- The top level can be parsed as either a map or a list, without the wrapping brackets.

```java
Parameter config = Parameter.parseMap(
        "title:'Main servers' servers:[{host:eu.example.com ports:[25565 25566]} {host:us.example.com}]"
);

config.get("title").textValue(); // "Main servers"
config.get("servers").count(); // 2
config.get("servers").get(0).get("ports").get(1).textValue(); // "25566"
config.get("servers").get(1).get("ports").isMissing(); // true
config.get("servers").get(7).get("host").get(0).isMissing(); // true, because there's no index 7 element
```
Lookups never return `null` or throw: anything absent is `Parameter.missing()`, which can be chained further.
Unquoted values that look like an `int`, `long`, `double` or `true`/`false` are parsed into their type, and still keep the exact text they were written with: `0.00000` has `textValue()` of `"0.00000"` and `asDouble(...)` of `0.0`.
Map keys are case-insensitive.

There is a single `Parameter` type: `isList()`, `isMap()` and `isPlain()` tell the kinds apart, and `isMissing()` tells an absent one.
`keys()` gives the keys to look up by: a map's keys, a list's indexes, or just `"0"` for a plain value.
`find(...)` returns an `Optional` instead of `missing()`, and `getMapped(...)` applies a function to the looked-up parameter.

The text of a parameter can be read with `getText(...)`, or converted with a fallback for absent and malformed values:
```java
Parameter config = Parameter.parseMap("name:Main port:25565 ratio:0.5 debug:true mode:fast");

config.getText("name"); // "Main"
config.get("port").asInt(); // OptionalInt[25565], empty if absent or malformed
config.get("port").asInt(0); // 25565
config.get("timeout").asInt(30); // 30, it's absent
config.get("ratio").asDouble(1.0); // 0.5
config.get("debug").asBoolean(); // true, only true/false are recognized; asBoolean(false) is the same
config.get("verbose").asTriState(); // TriState.UNSET, it's absent; accepts words like yes/on, see TriState
config.get("mode").asEnum(Mode.SLOW); // Mode.FAST, ignoring case; asEnum(Mode.class, def) works too
config.get("port").asInt(Integer::parseInt); // own parsing, receives "" if absent
config.get("id").as(UUID::fromString); // same for any type
```

A parameter can be turned back into a string with `serialize(...)`, which produces a normalized form that parses back to the same thing,
and compared with `matches(...)`, which ignores quoting, spacing and map entry order.
Parameters can also be built in code with `Parameter.ofValue(...)`, `Parameter.ofList(...)` and `Parameter.ofMap(...)`, or their no-arg forms for an empty one. `null` and missing entries are skipped.
`ofValue(...)` also takes an `int`, `long`, `double`, `boolean`, `TriState` or enum constant, which the matching `asX()` returns without parsing.
```java
Parameter config = Parameter.ofMap(Map.of(
        "title", Parameter.ofValue("Main servers"),
        "servers", Parameter.ofList(List.of(
                Parameter.ofMap(Map.of(
                        "host", Parameter.ofValue("eu.example.com"),
                        "ports", Parameter.ofList(List.of(Parameter.ofValue(25565), Parameter.ofValue(25566)))
                )),
                Parameter.ofMap(Map.of("host", Parameter.ofValue("us.example.com")))
        ))
));

config.get("title").textValue(); // "Main servers"
config.get("servers").get(0).get("ports").get(1).asInt(0); // 25566, without parsing
```
Entries keep the iteration order of the map given (which `Map.of` doesn't define; use sorted variant of `Map` when the order matters).
Classes can implement `Parameterizable` to provide their own parameter representation.

Parameters are immutable. `with(...)` returns a modified copy: it sets a key on a map or an index on a list, and returns the parameter as is for any other kind.
For several edits or removals, `editList()` and `editMap()` apply them in order with a single copy:
```java
Parameter list = Parameter.parseList("a b c");

list.with(1, Parameter.ofValue("B")); // [a B c]
list.with(4, Parameter.ofValue("e")); // [a b c '' e], the gap is filled with missing
Parameter.parseMap("mode:fast").with("MODE", Parameter.ofValue("slow")); // {mode:slow}, an absent value removes the key

list.editList()
        .insert(1, Parameter.ofValue("x"))
        .remove(0)
        .finish(); // [x b c]
```

Nesting depth is limited to 512, configurable via the `ink.glowing.utils.params.maxDepth` system property.

### `primitive.TriState`
A boolean with a third `UNSET` state, for things like "not configured" or "doesn't matter".
Mirrors the `Optional` API: presence checks, fallbacks, `ifPresent`, `orElseThrow`.
```java
TriState flag = TriState.of("yes"); // TRUE
flag.asBoolean(false); // true
TriState.UNSET.asBoolean(false); // false, the fallback

// UNSET works as a wildcard filter
TriState requireOp = TriState.UNSET;
requireOp.isValidFor(player.isOp()); // true for any player
```
`TriState.of(String)` uses `TriState.Mapper.DEFAULT`, which understands `true/on/yes/allow/enable` and their negatives. Anything else is `UNSET`.

`TriState.Mapper` converts between states and strings with custom words.
Each state has a main name, used when converting back to a string. `TRUE` and `FALSE` can also have extra variants that are accepted when parsing.
Matching ignores case, and unknown strings or `null` give the fallback state (`UNSET` unless changed with `fallback(...)`).
```java
TriState.Mapper mapper = TriState.Mapper.builder()
        .name(TriState.TRUE, "allow")
        .name(TriState.FALSE, "deny")
        .name(TriState.UNSET, "default")
        .addVariants(true, "yes", "+")
        .addVariants(false, "no", "-")
        .build();

mapper.parse("YES"); // TRUE
mapper.parse("maybe"); // UNSET, the fallback
mapper.name(TriState.FALSE); // "deny"
mapper.strings(TriState.TRUE); // [allow, yes, +]
```
An existing mapper can be extended with `toBuilder()`, e.g. to add words to `Mapper.DEFAULT`.
`build()` throws if the same word is assigned to different states.

### `primitive.num.NumberUtils`
Helpers for working with numbers. Parsing returns a fallback instead of throwing on a malformed string.
```java
NumberUtils.parseInt("42", 0); // 42
NumberUtils.parseInt("many", 0); // 0, the fallback
NumberUtils.parseDouble(null, 1.5); // 1.5
```
There are `parseInt`, `parseLong` and `parseDouble`. Without a fallback they return an `OptionalInt`, `OptionalLong` or `OptionalDouble`.

### `hash`
`HashList` is an unmodifiable `List` with O(1) `contains`, `indexOf` and `lastIndexOf`, for when a list needs both order and fast lookups.
A custom fastutil `Hash.Strategy` can be used to change how elements are compared.
```java
HashList<String> ci = HashList.ofCustom(CaseInsensitive.strategy(), "Alpha", "Beta");
ci.contains("ALPHA"); // true
```
`CaseInsensitive` provides fastutil maps and sets with case-insensitive `String` keys that keep the original casing.

### `rng`
`RngUtils` has shortcuts for common random picks: a random element of an array or list, and a random number in a range
where bounds can be given in any order. All methods use `ThreadLocalRandom` by default or accept any `RandomGenerator`.
```java
String color = RngUtils.randomElement(List.of("red", "green", "blue"));
int roll = RngUtils.inRange(1, 7); // 1..6
```
`WeightedPicker` picks elements with probability proportional to their weight. It is immutable, so one picker can be reused.
```java
WeightedPicker<String> loot = new WeightedPicker.Composer<String>()
        .add("common", 9)
        .add("rare", 1)
        .finish();
String drop = loot.next(RngUtils.threadRandom()); // "common" 90% of the time
```
Pickers can also be created from a map of weights, or from a collection with a function that computes each element's weight.
Elements with zero or negative weight are never picked. If any element has an infinite weight, only such elements are picked.

### `time.DurationUtils`
Parses human-written durations like `1h 30m` into `java.time.Duration`.
```java
DurationUtils.parseDuration("1h 30m");
DurationUtils.parseDuration("90"); // no unit means seconds by default
DurationUtils.parseDuration("2min", Map.of("min", ChronoUnit.MINUTES)); // custom units
```
Default units: `ns`, `ms`, `s`, `m`, `h`, `d`, case-insensitive.

### `Comparison`
The result of a comparison as a constant: `BELOW`, `EQUAL` or `ABOVE`, instead of the sign of an `int`.
```java
Comparison.of(5, 10); // BELOW, by natural order
Comparison.of("b", "a"); // ABOVE
Comparison.of(String.CASE_INSENSITIVE_ORDER, "A", "a"); // EQUAL, with a comparator
Comparison.is(7, Comparison.EQUAL, 7); // true, reads as "7 is EQUAL 7"
Comparison.ofSignum(-42); // BELOW, for the result of compareTo or compare
```

### `EnumUtils`
Helpers for working with enums. Looking up a constant by name ignores case and returns a fallback when there is no match.
```java
EnumUtils.asEnum("fast", Mode.SLOW); // Mode.FAST
EnumUtils.asEnum("warp", Mode.SLOW); // Mode.SLOW, the fallback
EnumUtils.asEnum("warp", Mode.class); // Optional.empty(), there is no fallback
```

### `FluentUtils`
Helpers to act on a value inline, without temporary variables.
```java
Map<String, Integer> ids = FluentUtils.peek(new HashMap<>(), map -> map.put("admin", 0));
int length = FluentUtils.map(parameter.get("name").textValue(), String::length);

String name = FluentUtils.orElse(System.getenv("USER_NAME"), "guest");
Config config = FluentUtils.orElseGet(cachedConfig, Config::load); // load() is only called if cachedConfig is null

Optional<Duration> timeout = FluentUtils.attempt(() -> DurationUtils.parseDuration(input)); // empty if parsing throws
Duration limit = FluentUtils.attemptOrElse(() -> DurationUtils.parseDuration(input), Duration.ofMinutes(1)); // the fallback if parsing throws
```

### `TextUtils`
Index-aware search and replace, plus `char` array counterparts of common `String` methods.
```java
TextUtils.replaceEach("a-b-c", "-", index -> "[" + index + "]"); // "a[1]b[3]c"
TextUtils.findEach("1a 2a 3a", "a", index -> System.out.println(index)); // 1, 4, 7

char[] chars = "key:value".toCharArray();
int colonAt = TextUtils.indexOf(chars, ':'); // 3
TextUtils.substring(chars, 0, colonAt); // "key"
TextUtils.subarray(chars, colonAt + 1); // ['v', 'a', 'l', 'u', 'e']
```
An empty search string matches nothing.

## Get it ![Version](https://img.shields.io/github/v/tag/GlowingInk/InkyUtils?sort=semver&style=flat&label=release)
Versions in dependency sections may be outdated. Check the badge above for the latest one.

[fastutil](https://github.com/vigna/fastutil) is a `provided` dependency and must be available at runtime.
### Maven
Add to repositories
```xml
<repository>
    <id>glowing-ink</id>
    <url>https://repo.glowing.ink/releases</url>
    <!-- https://repo.glowing.ink/snapshots for snapshots -->
</repository>
```
Add to dependencies
```xml
<dependency>
    <groupId>ink.glowing.utils</groupId>
    <artifactId>inkyutils</artifactId>
    <version>0.3.0</version>
</dependency>
```
### Gradle
```kotlin
repositories {
    maven {
        url = uri("https://repo.glowing.ink/releases")
        // https://repo.glowing.ink/snapshots for snapshots
    }
}

dependencies {
    implementation("ink.glowing.utils:inkyutils:0.3.0")
}
```
