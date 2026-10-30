# Global Tech Stack
- Backend: Java 21 (Spring Boot 4.1.2) - Microservices Architecture
- Frontend: React (TypeScript + Vite)

# Architecture Rules
- Backend: Microservices applying Hexagonal Architecture, SOLID principles, and Clean Code.
  - `domain/`: Pure Java logic, Core Entities, and Domain Services. Completely decoupled from frameworks.
  - `ports/`: Interfaces defining Inbound (driving) and Outbound (driven) communications.
  - `adapters/`: Implementations of ports. Includes `/in` (REST Controllers, event consumers) and `/out` (Spring Data JPA repositories, external HTTP clients).
- Frontend: Feature-Driven / Clean Architecture applying SOLID principles and Clean Code.
  - `src/core/`: Global configurations, HTTP clients, contexts.
  - `src/shared/`: Reusable UI components and generic hooks/utils.
  - `src/features/[feature_name]/`: Encapsulated modules (components, hooks, services).

# Coding Style (Fullstack)
- Use concise, modular, and testable code. 
- Strict separation of concerns: Domains must never depend on infrastructure or frameworks.
- Avoid monolithic components or giant service classes. Prefer composition over inheritance.
- Write tests for all critical business logic and components.
- Follow consistent naming conventions for files, classes, and functions.
- Keep functions and methods small and focused on a single responsibility.
- Use meaningful and descriptive variable names.
- Avoid deep nesting; prefer early returns and guard clauses.

# Documentation Standards
- All code documentation must be written in English, using clear, professional, and technically accurate language.
- Follow the official documentation conventions of each technology:
  - Backend (Java 21 + Spring Boot): Use JavaDoc.
  - Frontend (React + TypeScript): Use JSDoc.
- Documentation must be meaningful, concise, accurate, and aligned with the actual implementation.
## JavaDoc Standards (Backend - Java 21 + Spring Boot)
  - Use JavaDoc (/** ... */) to document classes, interfaces, enums, records, constructors, and public methods when their responsibilities or contracts require clarification.
  - Document Spring components such as Controllers, Application Services, Use Cases, Adapters, Repositories, and Configuration classes when necessary.
  - Document domain entities, value objects, domain services, ports, and business rules, explaining their responsibilities and purpose within the Hexagonal Architecture.
  - For methods, use the appropriate JavaDoc tags:
    @param: Describe method parameters.
    @return: Describe the returned value, when applicable.
    @throws: Document relevant exceptions and the conditions under which they are thrown.
    @see: Reference related classes or methods when useful.
  
  - Document important business rules, validations, preconditions, postconditions, side effects, and architectural constraints.
  - Explain relevant Spring-specific behavior when it is not self-evident, such as transaction boundaries, propagation, asynchronous execution, caching, or event handling.
  - Do not document framework annotations whose behavior is already obvious unless additional context is necessary.
  - Avoid redundant JavaDoc for simple getters, setters, constructors, or self-explanatory methods.
## JSDoc Standards (Frontend - React + TypeScript)
  - Use JSDoc (/** ... */) for exported components, custom hooks, services, utility functions, and complex types when additional documentation is useful.
  - Document component responsibilities, props, return values, expected behavior, and relevant side effects.
  - Use appropriate tags such as @param, @returns, and @example when they add meaningful information.
  - Keep documentation consistent with TypeScript interfaces, types, and actual implementation.
  - Avoid documenting self-explanatory code or repeating information already expressed by TypeScript types.
## Inline Comments Standards (Backend and Frontend)
  - Use inline comments (//) only when necessary to explain:
  - Complex or non-obvious business logic.
  - Important architectural decisions and implementation trade-offs.
  - Complex algorithms or calculations.
  - Workarounds, edge cases, and unexpected behavior.
  - Security constraints and critical validations.
  - Technical decisions that cannot be easily understood from the code itself.
  - Explain WHY something is done instead of simply describing WHAT the code does.
  - Place comments as close as possible to the code they explain.
  - Use TODO and FIXME only when necessary, clearly explaining the pending work.
##  Documentation Quality and Maintenance
  - Never generate redundant, unnecessary, misleading, or outdated documentation.
  - Never use comments as a substitute for clean code, meaningful naming, or proper code structure.
  - Document public contracts, business rules, complex behavior, and relevant architectural decisions.
  - Do not invent behavior, assumptions, or exceptions that are not present in the implementation.
  - Whenever existing code is modified, update its documentation if its responsibilities, behavior, or contracts change.
  - Ensure documentation remains consistent with the Hexagonal Architecture and Feature-Driven Architecture.
  - Before completing any implementation, review the documentation to ensure it is accurate, complete, consistent, and maintainable.