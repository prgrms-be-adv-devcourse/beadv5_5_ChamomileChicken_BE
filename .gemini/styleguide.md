# Gemini Code Assist Style Guide

## Language
Please write all code reviews, pull request summaries, and help messages in Korean.

## Review Focus
This repository is a Java + Spring Boot backend project.
When reviewing code, please focus on the following areas first:

1. Architecture and Layer Separation
- Check whether the responsibilities of Controller, Service, Repository, and Domain (Entity) are clearly separated.
- Check whether business logic is overly concentrated in Controllers.

2. DTO / Entity Separation
- Check whether request/response DTOs and Entities are being used for clearly different purposes.
- Make sure Entities are not exposed directly in ways that couple them to API specification changes.

3. Exception Handling
- Check whether exceptions have clear and meaningful purposes.
- Prefer specific exceptions over broad exception handling.

4. Transactions
- Check whether the scope of `@Transactional` is appropriate.
- For read-only queries, review whether `readOnly = true` is properly applied.

5. Performance
- Check for potential N+1 query issues.
- Check for unnecessary or repeated database queries.

6. Security
- Check whether authentication and authorization are properly handled.
- Make sure sensitive information such as tokens, passwords, or personal data is not written to logs.

7. Testability
- Review whether the code structure is easy to test with unit tests and integration tests.

8. Typos and Suspicious Parts
- Point out typos, awkward naming, misleading comments, or anything that looks unintended.
- If a piece of code appears suspicious or inconsistent, mention it as a possible issue even if it is not certain.

## Comment Style
- Prioritize issues that affect maintainability, stability, performance, and security over minor style suggestions.
- However, also point out typos, awkward expressions, and suspicious-looking code when they may cause confusion.
- Whenever possible, explain both the problem and a suggested improvement.
- If something is uncertain, avoid stating it as a fact and present it as a suggestion instead.