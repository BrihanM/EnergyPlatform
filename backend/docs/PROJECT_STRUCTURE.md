# Estructura del proyecto

`services/` contiene aplicaciones independientes. Cada servicio tiene su propio `pom.xml`, clase de arranque, configuración y pruebas. El POM raíz sólo agrega módulos y centraliza versiones; ningún servicio depende de otro como dependencia Maven.

Los servicios que implementen dominio seguirán `domain/`, `application/` e `infrastructure/` dentro de su paquete base. `auth-service` utiliza `domain/model` para sus entidades puras, `application/port/out` para contratos de persistencia e `infrastructure/adapter/out/persistence` para JPA, repositorios y mappers. Las migraciones Flyway se ubican en `src/main/resources/db/migration`.

No se crean clases vacías para representar capas aún no utilizadas. Las entidades JPA permanecen en infraestructura y no se exponen como modelos de dominio.
