# Item Spawner loader ports

The existing Forge 1.20.1 project remains at the repository root. New loader targets are built independently with Java 25:

- `forge-26.3`: Minecraft 26.3, Forge 66.0.9
- `neoforge-26.2`: Minecraft 26.2, NeoForge 26.2.0.88 (latest stable; 26.3 builds are beta)
- `fabric-26.3`: Minecraft 26.3, Fabric Loader 0.19.5 and Fabric API 0.161.0

From a port directory, run `../gradlew build`. Archives include the loader and Minecraft version in their names.