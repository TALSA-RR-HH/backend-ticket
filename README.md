# Backend Tickets RRHH - TALSA

Sistema de gestión de tickets para el departamento de Recursos Humanos de TALSA. Backend desarrollado con Spring Boot que permite gestionar solicitudes, quejas y atenciones del personal.

## Descripción

Este proyecto es un sistema backend RESTful que permite:
- Gestión completa de tickets de atención
- Autenticación y autorización de usuarios
- Categorización de tickets por tipo de atención
- Seguimiento del estado de las solicitudes
- Generación de resúmenes y estadísticas
- Comunicación en tiempo real mediante WebSockets

## Tecnologías

- **Java 17**
- **Spring Boot 4.0.1**
- **Spring Data JPA** - Gestión de persistencia
- **Spring Security** - Autenticación y autorización
- **PostgreSQL** - Base de datos
- **Lombok** - Reducción de código boilerplate
- **JWT** - Tokens de autenticación
- **WebSocket** - Comunicación en tiempo real
- **SpringDoc OpenAPI** - Documentación de API
- **Maven** - Gestión de dependencias

## Estructura del Proyecto

```
src/main/java/com/talsa/rrhh/backend/
├── config/              # Configuraciones (DataInitializer, OpenAPI, WebSocket)
├── controller/          # Controladores REST
├── dto/                 # Objetos de transferencia de datos
├── entity/              # Entidades JPA (Ticket, Usuario)
├── enums/               # Enumeraciones (EstadoTicket, CategoriaAtencion, etc.)
├── exception/           # Manejo de excepciones
├── repository/          # Repositorios JPA
├── security/            # Configuración de seguridad
└── service/             # Lógica de negocio
```

## Requisitos Previos

- Java JDK 17 o superior
- PostgreSQL 12 o superior
- Maven 3.6 o superior

## Configuración

1. **Clonar el repositorio**
```bash
git clone <url-del-repositorio>
cd backend-tickets-rrhh
```

2. **Configurar la base de datos**

Crear una base de datos PostgreSQL:
```sql
CREATE DATABASE tickets_db;
```

3. **Configurar application.properties**

Editar `src/main/resources/application.properties` con tus credenciales:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tickets_db
spring.datasource.username=postgres
spring.datasource.password=tu_contraseña
```

4. **Compilar el proyecto**
```bash
mvnw clean install
```

5. **Ejecutar la aplicación**
```bash
mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

## Documentación de la API

Una vez iniciada la aplicación, la documentación interactiva de la API estará disponible en:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Características Principales

### Gestión de Tickets
- Creación de tickets con diferentes categorías de atención
- Actualización del estado de los tickets
- Finalización de tickets con resoluciones
- Paginación y filtrado de tickets

### Categorías de Atención
- Consultas generales
- Quejas y reclamos
- Solicitudes de documentos
- Atención médica
- Otros servicios de RRHH

### Estados de Ticket
- Pendiente
- En proceso
- Finalizado
- Cancelado

### Lugares de Atención
- Presencial
- Virtual
- Telefónico

## Seguridad

El sistema implementa:
- Autenticación basada en JWT
- Roles de usuario (Admin, Usuario, Operador)
- Protección de endpoints mediante Spring Security
- Validación de datos de entrada

## Base de Datos

El sistema utiliza PostgreSQL con las siguientes entidades principales:
- **Usuario**: Gestión de usuarios del sistema
- **Ticket**: Registro de tickets de atención

Las tablas se generan automáticamente mediante JPA/Hibernate con la configuración `spring.jpa.hibernate.ddl-auto=update`.

## Compilación y Empaquetado

Para generar el JAR ejecutable:
```bash
mvnw clean package
```

El archivo JAR se generará en: `target/backend-tickets-rrhh-0.0.1-SNAPSHOT.jar`

Para ejecutar el JAR:
```bash
java -jar target/backend-tickets-rrhh-0.0.1-SNAPSHOT.jar
```

## Testing

Ejecutar los tests:
```bash
mvnw test
```

## Notas de Desarrollo

- El proyecto utiliza Lombok, asegúrate de tener el plugin instalado en tu IDE
- La primera ejecución inicializará la base de datos con datos de prueba (DataInitializer)
- Los logs de SQL están habilitados para facilitar el debugging

## Licencia

Este proyecto es propiedad de TALSA.

## Autor

**Creado por Valentín Fernández - 2026**

---