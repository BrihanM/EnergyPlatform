# Guías de desarrollo

- Use nombres descriptivos: clases y enums en `PascalCase`, métodos, variables y paquetes en `camelCase`/minúsculas, constantes en `UPPER_SNAKE_CASE`.
- Mantenga paquetes por responsabilidad y respete `domain -> application -> infrastructure` hacia el interior.
- Use inyección por constructor y dependencias orientadas a interfaces. No use estado global mutable.
- Los DTOs son contratos de entrada/salida; no exponga entidades de dominio directamente.
- Valide entradas en el borde y traduzca errores mediante excepciones específicas y respuestas consistentes. No capture excepciones ampliamente ni oculte fallos.
- Use logging estructurado con correlation ID; nunca registre secretos, tokens, contraseñas o credenciales.
- Configure valores externos mediante variables de entorno y perfiles, sin secretos en el repositorio.
- Cubra reglas de dominio con pruebas unitarias y adaptadores con pruebas de integración. Mantenga las pruebas deterministas.
- Documente APIs y decisiones no obvias con JavaDoc. Evite comentarios que repitan el código.
- Aplique SOLID, Clean Code, funciones pequeñas, guard clauses y responsabilidad única.
