# LineScheduleNotifier

This is the backend application for the LineScheduleNotifier, a LINE bot built with Ktor. It handles webhooks from the LINE Messaging API to respond to user commands and provide information.

## Features

*   **Webhook Handling:** Processes incoming webhook events from the LINE Platform.
*   **Command-based Replies:** Responds to specific text commands sent by users in one-on-one or group chats.
*   **Supported Commands:**
    *   **Schedule:** Retrieves and displays schedule information.
    *   **General Info:** Provides general rules and information.
    *   **ID Retrieval:** Responds with the user's or group's ID for debugging purposes.

## Prerequisites

Before you begin, ensure you have the following tools installed:

*   **JDK 17** or higher.
*   **Docker** (Optional, for containerized deployment).
*   **jq** (Required for local testing). A command-line JSON processor.

## Building and Running

This project uses the Gradle wrapper (`gradlew`), so you don't need to install Gradle separately.

### Running the Application Locally

To run the server, use the following command:
```shell
./gradlew run
```
The server will start on `http://localhost:8080`.

### Running Tests

To execute the unit tests for the project, run:
```shell
./gradlew test
```

### Building the Project

To build the project and create the necessary artifacts, run:
```shell
./gradlew build
```

## Running with Docker (Local)

To verify the Docker image and run the application in a containerized environment locally, use Docker Compose.
This setup mounts the local `src/main/resources` directory, allowing the container to use `application-local.yaml` for local-specific configurations.

### Authentication for GitHub Packages

This project depends on libraries hosted on GitHub Packages. To build the Docker image locally, you must provide your GitHub credentials so that Gradle can download these dependencies.

1.  **Create a Personal Access Token (PAT)** on GitHub with the `read:packages` scope.
2.  In the root of the project, create a file named `github-credentials.txt`.
3.  Add the following content to the file, replacing the placeholders with your actual username and the PAT you just created:

    ```
    export GITHUB_USER=<Your GitHub Username>
    export GITHUB_TOKEN=<Your GitHub Personal Access Token>
    ```

This file is used by `docker-compose.yml` to securely pass the credentials to the Docker build process. It is listed in `.gitignore` and should not be committed to version control.

### Starting the Container

To build the image and start the container, provide the `PROJECT_NUMBER` and optionally the `APP_LOCALE_LANGUAGE` as environment variables.

For example, to run the application in Japanese:
```shell
PROJECT_NUMBER=111111111111 APP_LOCALE_LANGUAGE=ja docker compose up --build
```
Supported locales are `en` (English) and `ja` (Japanese). If `APP_LOCALE_LANGUAGE` is not specified, it defaults to `en`.

### Stopping the Container

To stop the container and remove the created resources:

```shell
docker compose down
```

## API Endpoint

### `POST /webhook`

This is the main endpoint that receives all events from the LINE Messaging API.

*   **Method:** `POST`
*   **URL:** `/webhook`
*   **Description:** All incoming messages and events from LINE are sent to this endpoint. The server validates the request using the `X-Line-Signature` header and processes the event.

### `POST /push`

This endpoint triggers a proactive push notification from the bot (e.g., a daily schedule reminder).

*   **Method:** `POST`
*   **URL:** `/push`

## Local Testing

You can test the API endpoints by sending requests to the local server.

### Simulating a Webhook (`/webhook`)

The command below uses `jq` to dynamically set the message text within a template file and pipes the result to `curl`. This simulates a user sending a message.

**For User Messages:**
```shell
# Replace "schedule" with any command you want to test
jq '.events[0].message.text = "schedule"' src/test/resources/request/webhook/user_message.json | \
curl -v -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -H "X-Line-Signature: dummy_signature" \
  -d @-
```

**For Group Mentions:**
```shell
# Replace "@BotName schedule" with any command you want to test
jq '.events[0].message.text = "@BotName schedule"' src/test/resources/request/webhook/group_mention.json | \
curl -v -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -H "X-Line-Signature: dummy_signature" \
  -d @-
```

### Triggering a Push Notification (`/push`)

To test the push notification functionality, use the following `curl` command:
```shell
curl -v -X POST http://localhost:8080/push
```

### Available Commands for Webhook Testing

You can replace the message text in the webhook test commands with any of the following valid keywords (they are not case-sensitive):

*   **Schedule:** `schedule`, `スケジュール`, `予定`
*   **General Info:** `general info`, `info`, `rules`, `やくそく`, `ルール`, `持ち物`, `約束`, `お知らせ`
*   **User ID:** `user id`, `user_id`, `userid`, `ユーザーid`, `my id`, `あなたのid`
*   **Group ID:** `group id`, `group_id`, `groupid`, `グループid`
