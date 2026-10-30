# ADR-002: Arquitectura hexagonal

## Estado
Aceptada

## Decisión
Los servicios de negocio separarán `domain`, `application` e `infrastructure`. El dominio permanece libre de frameworks; la aplicación define puertos; infraestructura implementa adaptadores.

## Consecuencia
Las reglas se prueban sin infraestructura y las tecnologías pueden sustituirse con menor impacto.
