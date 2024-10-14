# WebApp Spring Boot Application

This is a Spring Boot web application that connects to a PostgreSQL database. The application uses environment variables to configure database credentials and URL.

## Prerequisites

Install the following on your machine:

- Java 17
- PostgreSQL
- Git

## Build and Deploy Instructions (Local)

Follow these steps to build and run the application locally.

### Step 1: Clone the Repository

### Step 2: Set Up Environment Variables

Set the following environment variables:

| Variable       | Description                                                                                                |
|----------------|------------------------------------------------------------------------------------------------------------|
| `DB_URL`       | The JDBC URL for the PostgreSQL database (e.g., `jdbc:postgresql://localhost:portnumber/yourdatabasename`) |
| `DB_USERNAME`  | The username for the PostgreSQL database (e.g., `yourusername`)                                            |
| `DB_PASSWORD`  | The password for the PostgreSQL database (e.g., `yourpassword`)                                            |

#### Env Variables setup instructions MacOS/Linux

1. Open your terminal and add the following lines to your shell configuration file (e.g., `~/.bash_profile`, `~/.zshrc`):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/postgres
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
```

2. After editing, reload the configuration with:

```bash
source ~/.zshrc
```

3. Confirm that the environment variables are set by running:
It should display the values of the env variables

```bash
echo $DB_URL $DB_USERNAME $DB_PASSWORD
```

### Step 3: Run the Application

To start the Spring Boot application, use your preferred IDE and open the project on the webapp folder and run WebappApplication Java file


The application should now be running on `http://localhost:8080`.

### API's:

You can verify that the application is running and properly connected to the database by accessing the `/healthz` endpoint.

1. Open a terminal and run the following command to test the health check API:

```bash
curl http://localhost:8080/healthz
```

The response should be:
- **200 OK** if the database connection is successful.
- **503 Service Unavailable** if the database connection fails.
- **400 Bad Request** if the request method is not supported like PUT,POST,etc.
