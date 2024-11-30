# Task Requirement
The test should be done using the Java Spring Boot framework and MySQL database.

## Task 1:

Develop an endpoint called ‘/import’ that accepts a CSV file that contains data of the following columns:
- id (a running number starts with 1)
- game_no (an integer value between 1 to 100)
- game_name (a string value not more than 20 characters)
- game_code (a string value not more than 5 characters)
- type (an integer, 1 = Online | 2 = Offline)
- cost_price (decimal value not more than 100)
- tax (9%)
- sale_price (decimal value, cost_price inclusive of tax)
- date_of_sale (a timestamp of the sale)

## Task 2:

- Design and create a database table called ‘game_sales’ that will store the content of the CSV.
- Design and create the necessary tables to track the progress of the CSV import.

## Task 3:

Design and develop an endpoint called ‘/getGameSales’ that returns the following result:
- A list of game sales
- A list of game sales during a given period (From date and To date).
- A list of game sales where sale_price is less than or more than a given parameter.

The endpoint should only return 100 records per request and must support pagination.
The endpoint should return results in less than 50ms when fetching from any page. (eg. page 999)

## Task 4:

Design and develop an endpoint called ‘/getTotalSales’ that returns the possible result:
- The total number of games sold during a given period. (eg. daily counts)
- The total sales generated (total sale_price) during a given period. (eg. daily sales)
- The total sales generated (total sale_price) during a given period with a given game_no. (eg. daily sales of a particular game_no)

The result should be pre-aggregated and should not count or sum on the fly.
The endpoint should return results in less than 50ms.


## Task 5:

- Prepare a CSV of 1,000,000 rows with random generated values based on the rules defined in Task 1.
- ‘date_of_sale’ values to be populated randomly between 1 April to 30 April.
- Load the CSV with 1,000,000 rows using ‘/import’ endpoint.

The import process should be completed in under 20 seconds.


## Assessment Criteria:

- API development
- Validation and Error handling
- Performance of the endpoints
- Scalability
- Database design
- Testing
- Critical thinking

You may use additional middleware to achieve the above outcome.

Please upload all relevant source codes to a github account and share with us the link to the repository.
In the repository, please include a README file that contains the build and run steps.

Please attach screenshots of the test result of all the endpoints containing the execution times.

# Game Sales Service

## Overview
The Game Sales Service is a backend Java Spring Boot application that allows the import of game sales data via a CSV file and provides various endpoints to query sales data. It aims to demonstrate effective API development, performance optimization, scalability, and validation using Spring Boot and MySQL.

### Key Features:
1. **Import Game Sales** - Upload a CSV file of game sales.
2. **Get Game Sales** - Retrieve game sales data with various filtering options.
3. **Get Total Sales** - Retrieve aggregated sales data based on the provided parameters.
4. **Caching** - Improved performance with in-memory caching.

### Data Model
The design uses two tables:
1. **game_sales**: Stores details of individual game sales.
2. **game_sales_aggregated**: Stores aggregated data, such as total sales for each day. This structure helps efficiently calculate and store frequently queried aggregate data, improving performance for large datasets.


## Prerequisites
1. **Java** - Java version 21 or above. You can install this using SDKMAN.
2. **Maven** - Apache Maven 3.9.9 or above.
3. **MySQL** - MySQL server installed locally.
4. **Postman** - For testing APIs.
5. **MySQL Workbench** - To easily interact with MySQL server.

---

## 1. Setting up the Environment

### 1.1 Clone the Repository
```sh
git clone <repository-url>
cd gameSalesService
```

### 1.2 Environment Variables
Refer to the `application.properties` file to configure your environment. The required settings are:
- MySQL configuration (URL, username, password)
- Caching properties

Ensure the following settings are adjusted in the `src/main/resources/application.properties` file:
```
spring.datasource.url=jdbc:mysql://localhost:3306/game_sales_db?rewriteBatchedStatements=true
spring.datasource.username=gamesales_user
spring.datasource.password=password123
```

### 1.3 Dependencies Installation
To install dependencies, navigate to the project root and run:
```sh
mvn clean install
```

---

## 2. Setting Up MySQL Server

### 2.1 MySQL Installation
- **Windows**: Use the MySQL installer from [MySQL Downloads](https://dev.mysql.com/downloads/installer/).
- **Mac**: Use Homebrew to install MySQL:
  ```sh
  brew install mysql
  ```

### 2.2 Database Setup
Create a new database named `game_sales_db`:
```sql
CREATE DATABASE game_sales_db;
```
Create a user and grant privileges:
```sql
CREATE USER 'gamesales_user'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON game_sales_db.* TO 'gamesales_user'@'localhost';
```

### 2.3 MySQL Workbench
Use MySQL Workbench to interact with the database for visualization and running SQL scripts.

---

## 3. Set Up Postman for Testing

### 3.1 Installation
- **Windows/Mac**: Download and install Postman from [Postman Downloads](https://www.postman.com/downloads/).

### 3.2 Testing GameController API
- **Import CSV File**:
   - Endpoint: `POST /api/import`
   - Headers: `Content-Type: multipart/form-data`
   - Body: Select the CSV file you want to import.
- **Get Game Sales**:
   - Endpoint: `GET /api/getGameSales`
   - Parameters: `fromDate`, `toDate`, `salePrice`, `filter`, `page`, `size`.
   - Test different scenarios such as providing date ranges or price filters to see paginated results.
- **Get Total Sales**:
   - Endpoint: `GET /api/getTotalSales`
   - Parameters: `fromDate`, `toDate`, `gameNo`, `filter`.

Ensure that you set the parameters in the Postman request to match the different test scenarios provided in the requirements.

---

## 4. Running on Mac and Windows

### 4.1 Start MySQL Server
- **Windows**: Start MySQL server through the MySQL service.
- **Mac**:
  ```sh
  mysql.server start
  ```

### 4.2 Running the Application
Navigate to the root directory and run the following command:
```sh
mvn spring-boot:run
```

### 4.3 Differences in Setup Commands
- **MySQL Installation**: Windows uses an installer while macOS uses Homebrew.
- **Starting MySQL**: Mac requires using `mysql.server start`.

---

## 5. Summary of Task Requirements

1. **Task 1**: Develop the `/import` endpoint to accept a CSV file containing game sales data with the specified columns.
2. **Task 2**: Design and create the necessary tables (`game_sales`, progress-tracking table).
3. **Task 3**: Create `/getGameSales` endpoint to return paginated game sales results with filtering options (by date, price).
4. **Task 4**: Create `/getTotalSales` to return the total number of games sold or total sales based on different criteria.
5. **Task 5**: Prepare and import a CSV of 1,000,000 rows to test performance.

---

## 6. Approach

- **Caching**: Simple in-memory caching (`spring.cache.type=simple`) is used to improve response time, particularly for frequently requested pages in the `/getGameSales` endpoint.
- **Concurrency for Import**: Uses multiple threads to quickly process and save data from the imported CSV file.
- **Performance Optimization**: Pre-loading cache during application startup with the use of multi-threaded loading.

--- 
### Summary of API Endpoints
1. **`/api/import`**: Imports game sales data from a CSV file.
2. **`/api/getGameSales`**: Retrieves game sales with optional filtering and pagination.
3. **`/api/getTotalSales`**: Retrieves the total sales or sales count for a specified period.

