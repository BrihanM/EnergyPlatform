# ADR-004: API Gateway

## Estado
Aceptada

## Decisión
El Gateway será el punto de entrada de clientes externos y centralizará routing, controles perimetrales, trazabilidad y auditoría de metadatos. No contendrá reglas de negocio.

## Seguridad
Los logs de auditoría excluirán contraseñas, tokens, credenciales y cualquier información sensible.
