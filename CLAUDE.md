# Mi App Fullstack - Claude Guidelines
- Tech Stack: Java 21 (Spring Boot 4.1.2) & React (TypeScript + Vite)
- Architecture: Modular Monorepo. 
  - Backend: Domain-Driven Design (DDD).
  - Frontend: Feature-Driven / Clean Architecture applying SOLID principles.
- Frontend Structure Pattern:
  - `src/core/`: Global configurations, HTTP clients, contexts, security (Dependency Inversion).
  - `src/shared/`: Reusable UI components (buttons, inputs) and generic hooks/utils.
  - `src/features/[feature_name]/`: Encapsulated modules (e.g., auth, users, products). Each feature has its own `/components`, `/hooks`, and `/services`.
- Token Optimization: Use concise code diffs. NEVER print or read full classes/components.
