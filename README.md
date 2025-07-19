# RAG Demo with Spring AI

This project is a Retrieval-Augmented Generation (RAG) application built with Spring Boot and Spring AI. It ingests PDF documents from a local directory, stores them in a Chroma vector database, and uses an OpenAI model to answer questions based on the content of those documents.

## Features

- **PDF Ingestion**: Automatically reads and processes PDF files from a specified directory at startup.
- **Vector Store**: Uses ChromaDB to store document embeddings for efficient similarity searches.
- **RAG Implementation**: Augments user prompts with relevant context from the ingested documents before calling the language model.
- **OpenAI Integration**: Connects to OpenAI's API to leverage powerful models like GPT-4.1-nano.

## Technologies Used

- **Java 21**
- **Spring Boot 3.5.3**
- **Spring AI 1.0.0**
- **OpenAI**: `gpt-4.1-nano`
- **ChromaDB**: Vector Store
- **Apache PDFBox**: For PDF text extraction
- **Gradle**: Build tool

## Getting Started

### Prerequisites

- Java 21 or later
- An active OpenAI API key
- Docker (for running the ChromaDB instance, if not already running)

### Configuration

1.  **Set OpenAI API Key**: Open the `src/main/resources/application.properties` file and replace `your-api-key` with your actual OpenAI API key:

    ```properties
    spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key}
    ```

    Alternatively, you can set it as an environment variable named `OPENAI_API_KEY`.

2.  **Configure PDF Directory**: In the same `application.properties` file, update the `pdf.ingest.dir` property to point to the directory containing the PDF files you want to ingest:

    ```properties
    pdf.ingest.dir=/path/to/your/pdfs/
    ```

### Running the Application

Once configured, you can run the application using the Gradle wrapper:

```bash
./gradlew bootRun
```

The application will start, ingest the PDFs, and be ready to accept requests.

## API Endpoint

To chat with the RAG model, send a GET request to the `/chat` endpoint with your message as a query parameter.

**Example using `curl`:**

```bash
curl "http://localhost:8080/chat?message=What is the main topic of the documents?"
```
