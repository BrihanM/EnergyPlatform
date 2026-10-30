# ADR-003: Propiedad de datos

## Estado
Aceptada

## Decisión
PostgreSQL será el motor relacional principal. Podrá existir una instancia compartida, pero cada microservicio será propietario lógico de sus datos y esquema; otros servicios no accederán directamente a sus tablas.

Redis será caché o almacenamiento temporal opcional y nunca requisito de la lógica fundamental del dominio.
