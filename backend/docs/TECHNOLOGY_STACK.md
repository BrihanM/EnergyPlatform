# Stack tecnológico

| Tecnología | Decisión |
|---|---|
| Java | 21 LTS |
| Spring Boot | 4.1.2 |
| Spring Cloud | Release train 2025.1.2 |
| Build | Maven multi-módulo con Wrapper |
| Gateway | Spring Cloud Gateway WebFlux |
| Datos | PostgreSQL futuro, con propiedad lógica por servicio |
| Caché | Redis futuro y opcional para el dominio |

Spring Boot 4.1.2 requiere Java 17+ y Maven 3.6.3+; Java 21 es la versión objetivo. Spring Cloud 2025.1.2 es el release train compatible documentado con Boot 4.1.x. Las versiones se centralizan en el POM raíz.
