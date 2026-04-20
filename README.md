# ConvertitoreFile

A Java-based file conversion engine with a web service interface and a GUI. Supports a wide range of format conversions — spreadsheets, documents, PDFs, images, archives, and email files.

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Spring](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=spring&logoColor=white)

---

## Supported Conversions

| Input | Output |
|-------|--------|
| CSV | JSON |
| JSON | XLSX, XLS, ODS |
| XLSX / XLS / ODS | JSON |
| TXT | PDF, DOCX |
| PDF | DOCX, DOC, JPG |
| EML / MSG | PDF |
| Images | Various formats |
| ZIP | TAR.GZ |
| TAR.GZ | ZIP |

---

## Architecture

```
GUI (JavaFX)
    ↕
Engine (core converter logic)
    ↕ (optional)
Web Service (Spring Boot REST API)
```

- `Engine.java` — orchestrates conversion jobs, with logging and error handling
- `DirectoryWatcher.java` — watches an input folder and converts files automatically on drop
- Each format has a dedicated converter class implementing the `Converter` interface
- `AbstractPDFConverter` — shared base class for PDF-related conversions
- A Spring Boot web service (`EngineWebService`) exposes the engine as a REST API

---

## Build & Run

### Prerequisites

- Java 17+
- Maven

### Build

```bash
./mvnw clean package
```

### Run (GUI mode)

```bash
java -jar target/convertitoreFile.jar
```

### Run (Web Service mode)

```bash
./mvnw spring-boot:run
```

---

## Project context

School project at IIS B. Castelli, Brescia.  
Focus: object-oriented design with interfaces and abstract classes, web services in Java, and integrating multiple format conversion libraries.

**Author:** Simone Rossi
