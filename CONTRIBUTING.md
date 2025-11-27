# Contributing to Yaci Store

Thank you for your interest in contributing to Yaci Store! We welcome contributions from the community and are grateful for any help you can provide.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive criticism
- Respect differing viewpoints and experiences

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Create a branch** for your changes
4. **Make your changes** and commit them
5. **Push to your fork** and submit a pull request

## How to Contribute

### Reporting Bugs

Before reporting a bug:
- Check the [existing issues](https://github.com/bloxbean/yaci-store/issues) to avoid duplicates
- Verify the bug exists in the latest version

When reporting:
- Use a clear and descriptive title
- Describe the exact steps to reproduce
- Include your environment details (Java version, OS, database)
- Provide relevant logs or error messages

### Suggesting Enhancements

We welcome feature suggestions! Please:
- Check if the feature has already been suggested
- Provide a clear use case
- Explain why this enhancement would be useful
- Consider if it can be implemented as a plugin

### Contributing Code

1. **Find an issue** labeled `good first issue` or `help wanted`
2. **Comment on the issue** to let others know you're working on it
3. **Ask questions** if you need clarification
4. **Submit a pull request** when ready

## Development Setup

### Prerequisites

- Java 21 or higher
- Gradle 8.x
- Git
- Your preferred IDE (IntelliJ IDEA recommended)

### Building the Project

```bash
git clone https://github.com/bloxbean/yaci-store.git
cd yaci-store
./gradlew clean build
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :stores:utxo:test
```

### Running Locally

```bash
# Edit config/application.properties with your settings

# Run the application
java -jar applications/all/build/libs/yaci-store-all-*.jar
```

For running from source using Docker, see the guide at `docker/RUN_FROM_SOURCE.md` or:  
https://github.com/bloxbean/yaci-store/blob/main/docker/RUN_FROM_SOURCE.md

## Pull Request Process

1. **Update your branch** with the latest main branch
2. **Ensure all tests pass** locally
3. **Add tests** for new functionality
4. **Update documentation** if needed
5. **Write clear commit messages** 
6. **Create a pull request** with a clear description

### PR Description Template

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Tests pass locally
- [ ] New tests added
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project standards
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings
```

## Coding Standards

### Java Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and small
- Add Javadoc for public APIs
- Use appropriate logging levels

### Code Organization

- Place new stores in the `stores` module
- Follow existing package structure
- Keep related functionality together
- Maintain clear separation of concerns

### Dependencies

- Minimize external dependencies
- Document why new dependencies are needed
- Keep dependencies up to date
- Avoid version conflicts

## Testing Guidelines

### Unit Tests

- Write unit tests for all new functionality
- Aim for high code coverage
- Use meaningful test names
- Test edge cases

### Integration Tests

- Test database interactions
- Verify API endpoints
- Test plugin functionality
- Ensure backward compatibility

### Test Structure

```java
@Test
void shouldDescribeWhatIsBeingTested() {
    // Given - setup test data
    
    // When - execute the action
    
    // Then - verify the result
}
```

## Documentation

### Code Documentation

- Add Javadoc to public methods and classes
- Include examples where helpful
- Document complex algorithms
- Keep documentation up to date

### User Documentation

- Update relevant .mdx files in `/docs`
- Include configuration examples
- Add to FAQ if applicable

### API Documentation

- Document new endpoints
- Include request/response examples
- Note any breaking changes
- Update OpenAPI specifications

## Community

### Getting Help

- **Discord**: Join our [Discord server](https://discord.gg/JtQ54MSw6p)
- **Discussions**: Use [GitHub Discussions](https://github.com/bloxbean/yaci-store/discussions)
- **Issues**: Check existing [issues](https://github.com/bloxbean/yaci-store/issues)

### Ways to Contribute Beyond Code

- Improve documentation
- Answer questions in discussions
- Review pull requests
- Share your use cases
- Write tutorials or blog posts
- Report bugs
- Suggest features

## Questions?

If you have questions about contributing, please:
1. Check the [documentation](https://store.yaci.xyz)
2. Ask in [GitHub Discussions](https://github.com/bloxbean/yaci-store/discussions)
3. Join our [Discord server](https://discord.gg/JtQ54MSw6p)

Thank you for contributing to Yaci Store! 🎉