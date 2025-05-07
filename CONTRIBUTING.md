# Contributing to AstroNavigation

Thank you for considering contributing to AstroNavigation! This document provides guidelines and instructions for contributing to this project.

## Code of Conduct

Please be respectful and considerate of others when contributing to this project. We aim to foster an inclusive and welcoming community.

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with the following information:
- A clear, descriptive title
- Steps to reproduce the bug
- Expected behavior
- Actual behavior
- Screenshots (if applicable)
- Device information (Android version, device model)
- Any additional context

### Suggesting Features

If you have an idea for a new feature, please create an issue with:
- A clear, descriptive title
- A detailed description of the proposed feature
- Any relevant mockups or examples
- Why this feature would be beneficial

### Pull Requests

1. Fork the repository
2. Create a new branch (`git checkout -b feature/your-feature-name`)
3. Make your changes
4. Run tests to ensure your changes don't break existing functionality
5. Commit your changes (`git commit -m 'Add some feature'`)
6. Push to the branch (`git push origin feature/your-feature-name`)
7. Create a new Pull Request

## Development Setup

Please refer to the [Development Setup](README.md#development-setup) section in the README for instructions on setting up the development environment.

## Testing

Before submitting a pull request, please run the tests to ensure your changes don't break existing functionality:

```
# Run KotlinTranslation tests
gradlew :KotlinTranslation:test

# Run StarApp tests
gradlew :StarApp:app:runUnitTests
```

## Coding Style

- Follow the existing code style in the project
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new functionality

## License

By contributing to this project, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).