# ADR-001: Arquitectura de microservicios

## Estado
Aceptada

## Decisión
Cada dominio se implementará como servicio independiente, con ciclo de vida, despliegue y persistencia propios. El monorepo facilita gobierno y construcción conjunta sin convertir los servicios en módulos con dependencias de código compartido.

## Consecuencia
Los contratos entre servicios deben ser explícitos y versionables. Se acepta el coste operativo adicional para preservar autonomía.
