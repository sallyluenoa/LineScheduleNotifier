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
