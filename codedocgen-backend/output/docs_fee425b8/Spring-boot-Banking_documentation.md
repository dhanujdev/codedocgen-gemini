# Project: Spring-boot-Banking

## Overview
Project Name: Spring-boot-Banking.
Type: Maven.
Spring Boot Version: 2.7.0.
Contains 25 classes/interfaces/enums. (Controllers: 2, Services: 2, Repositories: 2).
Exposes 5 API endpoints. (5 REST).
An OpenAPI (Swagger) specification is available for REST APIs.


## Classes

### .MavenWrapperDownloader (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\.mvn\wrapper\MavenWrapperDownloader.java`

### com.example.paul.Application (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\Application.java`

### com.example.paul.constants.ACTION (`enum`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\constants\ACTION.java`

### com.example.paul.constants.constants (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\constants\constants.java`

### com.example.paul.controllers.AccountRestController (`controller`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\controllers\AccountRestController.java`

### com.example.paul.controllers.TransactionRestController (`controller`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\controllers\TransactionRestController.java`

### com.example.paul.models.Account (`entity`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\models\Account.java`

### com.example.paul.models.Transaction (`entity`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\models\Transaction.java`

### com.example.paul.repositories.AccountRepository (`repository`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\repositories\AccountRepository.java`
- **Extends:** `JpaRepository`

### com.example.paul.repositories.TransactionRepository (`repository`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\repositories\TransactionRepository.java`
- **Extends:** `JpaRepository`

### com.example.paul.services.AccountService (`service`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\services\AccountService.java`

### com.example.paul.services.TransactionService (`service`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\services\TransactionService.java`

### com.example.paul.utils.AccountInput (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\AccountInput.java`

### com.example.paul.utils.CodeGenerator (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\CodeGenerator.java`

### com.example.paul.utils.CreateAccountInput (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\CreateAccountInput.java`

### com.example.paul.utils.DepositInput (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\DepositInput.java`

### com.example.paul.utils.InputValidator (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\InputValidator.java`

### com.example.paul.utils.TransactionInput (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\TransactionInput.java`

### com.example.paul.utils.WithdrawInput (`class`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\main\java\com\example\paul\utils\WithdrawInput.java`
- **Extends:** `AccountInput`

### com.example.paul.integration.CheckBalanceIntegrationTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\integration\CheckBalanceIntegrationTest.java`

### com.example.paul.integration.MakeTransferIntegrationTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\integration\MakeTransferIntegrationTest.java`

### com.example.paul.unit.AccountRestControllerTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\unit\AccountRestControllerTest.java`

### com.example.paul.unit.AccountServiceTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\unit\AccountServiceTest.java`

### com.example.paul.unit.TransactionRestControllerTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\unit\TransactionRestControllerTest.java`

### com.example.paul.unit.TransactionServiceTest (`test`)
- **File Path:** `C:\tmp\repos\repo_fee425b8\src\test\java\com\example\paul\unit\TransactionServiceTest.java`

## API Endpoints

### `POST /accounts`
- **Handler:** `com.example.paul.controllers.AccountRestController.checkAccountBalance`

### `PUT /accounts`
- **Handler:** `com.example.paul.controllers.AccountRestController.createAccount`

### `POST /transactions`
- **Handler:** `com.example.paul.controllers.TransactionRestController.makeTransfer`

### `POST /withdraw`
- **Handler:** `com.example.paul.controllers.TransactionRestController.withdraw`

### `POST /deposit`
- **Handler:** `com.example.paul.controllers.TransactionRestController.deposit`

## Diagrams

### USECASE_DIAGRAM
![USECASE_DIAGRAM](/generated-output/docs_fee425b8/diagrams/usecase_diagram.svg)

### ENTITY_RELATIONSHIP_DIAGRAM
![ENTITY_RELATIONSHIP_DIAGRAM](/generated-output/docs_fee425b8/diagrams/entity_relationship_diagram.svg)

### SEQUENCE_DIAGRAM
![SEQUENCE_DIAGRAM](/generated-output/docs_fee425b8/diagrams/sequence_diagram_com_example_paul_controllers_TransactionRestController_deposit_DepositInput_depositInput_.svg)

### COMPONENT_DIAGRAM
![COMPONENT_DIAGRAM](/generated-output/docs_fee425b8/diagrams/component_diagram.svg)

### CLASS_DIAGRAM
![CLASS_DIAGRAM](/generated-output/docs_fee425b8/diagrams/class_diagram.svg)

## OpenAPI Specification

```yaml
Error generating OpenAPI spec: Illegal character range near index 14
[^a-zA-Z0-9_-\.]
              ^
```

