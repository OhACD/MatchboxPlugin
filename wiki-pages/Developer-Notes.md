# Developer Notes

This page provides technical information for developers working on or integrating with Matchbox.

## Project Structure

### Main Package
- **Base**: `com.ohacd.matchbox`
- **API**: `com.ohacd.matchbox.api`

### Key Classes

#### Core Classes
- `Matchbox` — Main plugin class
- `GameManager` — Manages game sessions and state
- `SessionManager` — Handles multiple parallel sessions
- `PlayerManager` — Tracks player states and roles

#### API Classes
- `MatchboxAPI` — Main API entry point
- `SessionBuilder` — Fluent builder for creating sessions
- `ApiGameSession` — Session wrapper for external access
- `GameConfig` — Configuration builder

#### Managers
- `InventoryManager` — Handles player inventories and abilities
- `SkinManager` — Manages player skins during games
- `MessageUtils` — Title and message utilities

#### Listeners
- `PlayerListener` — Core game event handling
- `ChatListener` — Chat and hologram system
- `ProtectionListener` — Damage and block protection
- Various ability listeners (Hunter Vision, Spark Swap, etc.)

## Building the Project

### Requirements
- JDK 21 or higher
- Gradle 8.x (wrapper included)

### Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Generate Javadoc
./gradlew javadoc

# Clean build directory
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Testing

### Running Tests Locally
```bash
./gradlew test
```

### Test Coverage
- Unit tests in `src/test/java`
- API tests cover session creation, management, and events
- Performance tests validate concurrent session handling

### Writing Tests
- Follow existing test conventions
- Use JUnit 5 for test framework
- Mock Bukkit objects where necessary
- Add tests for any behavior changes

## API Development

### Using the API
See the [API Documentation](API) page for quick examples, or refer to `MatchboxAPI_Docs.md` in the repository for comprehensive documentation.

### API Stability
- Public API is versioned and maintained for backward compatibility
- Classes marked with `@Internal` are not part of the public API
- Classes marked with `@Experimental` may change between versions
- All public API methods have `@NotNull` / `@Nullable` annotations

## Code Conventions

### Style Guidelines
- Use Java naming conventions (camelCase for methods, PascalCase for classes)
- Keep methods focused and single-purpose
- Add Javadoc comments to public API methods
- Use meaningful variable and method names

### Best Practices
- Thread safety: Use `ConcurrentHashMap` for shared collections
- Resource management: Clean up sessions properly on shutdown
- Error handling: Use `Optional` for nullable returns, validate inputs
- Event system: Dispatch events for significant game state changes

## Contributing Workflow

1. **Discuss First**: For major changes, open an issue to discuss design
2. **Branch**: Create a feature branch from `main`
3. **Develop**: Make your changes following conventions
4. **Test**: Add tests and ensure all tests pass
5. **Document**: Update relevant documentation and CHANGELOG.md
6. **Submit**: Open a PR with clear description and test results

See [Contributing](Contributing) for more details.

## Resources

### Documentation
- **Full API Docs**: `MatchboxAPI_Docs.md` (in repository)
- **Changelog**: `CHANGELOG.md` (in repository)
- **Development Policy**: `DEVELOPMENT_POLICY.md` (in repository)

### External Dependencies
- **Paper API**: MC 1.21.10 (build against latest)
- **ProtocolLib**: 5.4.0+ (for Hunter Vision packet manipulation)

### Useful Links
- [Paper Documentation](https://docs.papermc.io/)
- [ProtocolLib Wiki](https://github.com/dmulloy2/ProtocolLib/wiki)
- [Gradle User Guide](https://docs.gradle.org/)

## Development Environment Setup

### IntelliJ IDEA
1. Import project as Gradle project
2. Set project SDK to JDK 21
3. Enable annotation processing
4. Run `./gradlew build` to fetch dependencies

### Eclipse
1. Import as existing Gradle project
2. Set JDK 21 as the compiler
3. Run `./gradlew eclipse` to generate Eclipse files

### VS Code
1. Install Java Extension Pack
2. Install Gradle extension
3. Open project folder
4. Let VS Code detect Gradle configuration

## Questions?

- Check the [Discord](https://discord.gg/BTDP3APfq8) for real-time help
- Review existing code and tests for examples
- Open an issue for design discussions
