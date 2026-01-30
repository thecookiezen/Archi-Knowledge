# Contributing to Memory MCP Server

First off, thank you for considering contributing to the project! 

## How Can I Contribute?

### Reporting Bugs

- Check if the bug has already been reported in the Issues.
- If not, create a new issue. Include a clear title, a description of the problem, steps to reproduce, and any relevant logs or screenshots.

### Suggesting Enhancements

- Check if the enhancement has already been suggested.
- Open a new issue with a clear title and description of the suggested enhancement.

### Pull Requests

1. **Fork the repository** and create your branch from `main`.
2. **Install dependencies**: Ensure you have Java 21 and Maven installed.
3. **Make your changes**:
    - Follow the existing code style.
    - Write clear, concise commit messages.
    - Add or update tests as necessary.
4. **Run tests**: Ensure all tests pass before submitting.
   ```bash
   mvn test
   ```
5. **Submit a Pull Request**: Provide a clear description of the changes and link to any relevant issues.

## Coding Standards

- **Java**: Follow standard Java coding conventions.
- **Testing**: We use JUnit 5 for testing. Aim for high test coverage for new features.
- **Documentation**: Update the `README.md` if you add new features or change configurations.

## Project Structure

- `src/main/java`: Source code.
- `src/test/java`: Test suites.
- `pom.xml`: Project dependencies and configuration.

## License

By contributing, you agree that your contributions will be licensed under its MIT License.
