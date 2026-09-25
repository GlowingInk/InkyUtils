# InkyUtils
Small Java 25 utility library built on top of [fastutil](https://github.com/vigna/fastutil): config-style parameter parsing, a three-state boolean, hash-backed collections, weighted randomness and duration parsing.

### `params`
Parses compact, human-written parameter strings (commands, config values, tags) into a tree of values, lists and maps.

- Values are separated by whitespace. `key:value` makes a map entry, `[...]` a list, `{...}` a nested map.
- Values containing whitespace, colons or brackets are quoted with `'...'`; `\` and `'` are escaped with `\`.
- The top level can be parsed as either a map or a list, without the wrapping brackets.

```java
Parameter.Mapped config = Parameter.Mapped.parse(
        "title:'Main servers' servers:[{host:eu.example.com ports:[25565 25566]} {host:us.example.com}]"
);

config.get("title").textValue(); // "Main servers"
config.get("servers").count(); // 2
config.get("servers").get(0).get("ports").get(1).textValue(); // "25566"
config.get("servers").get(1).get("ports").isMissing(); // true
config.get("servers").get(7).get("host").get(0).isMissing(); // true, no exceptions along the way
```
Lookups never return `null` or throw: anything absent is `Parameter.missing()`, which can be chained further.
Map keys are case-insensitive.

A parameter can be turned back into a string with `serialize(...)`, which produces a normalized form that parses back to the same thing,
and compared with `matches(...)`, which ignores quoting, spacing and map entry order.
Parameters can also be built in code with `Parameter.Value.of(...)`, `Parameter.Listed.of(...)` and `Parameter.Mapped.of(...)`.
Classes can implement `Parameterizable` to provide their own parameter representation.

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
Each state has a main name, used when converting back to a string, and any number of extra variants that are also accepted when parsing.
Matching ignores case, and unknown strings or `null` give the fallback state (`UNSET` unless changed with `fallback(...)`).
```java
TriState.Mapper mapper = TriState.Mapper.builder()
        .main(TriState.TRUE, "allow")
        .main(TriState.FALSE, "deny")
        .main(TriState.UNSET, "default")
        .addVariants(TriState.TRUE, "yes", "+")
        .addVariants(TriState.FALSE, "no", "-")
        .build();

mapper.byString("YES"); // TRUE
mapper.byString("maybe"); // UNSET, the fallback
mapper.toString(TriState.FALSE); // "deny"
```
An existing mapper can be extended with `toBuilder()`, e.g. to add words to `Mapper.DEFAULT`.
`build()` throws if the same word is assigned to different states.

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
String drop = loot.next(RngUtils.threadRandom()); // "common" ~90% of the time
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

### Misc
- `FluentUtils` - `peek` and `map` helpers to act on a value inline without a temporary variable.

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
    <version>0.1.0-SNAPSHOT</version>
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
    implementation("ink.glowing.utils:inkyutils:1.0.0-SNAPSHOT")
}
```
