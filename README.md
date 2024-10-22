# Trade Tracker API

## Project Description

The Trade Tracker API is a tool designed to help users track their trades, analyze market data, and manage their portfolios. It provides various endpoints for retrieving historical data, subscribing to ticker updates, and calculating technical indicators such as SMA and RSI.

## Setup Instructions

1. Clone the repository:
   ```sh
   git clone https://github.com/beem812/trade-tracker-api.git
   cd trade-tracker-api
   ```

2. Install dependencies:
   ```sh
   sbt update
   ```

3. Run the project:
   ```sh
   sbt run
   ```

## API Endpoints

### GET /trades
Retrieve a list of all trades.

### GET /{ticker}/costbasis
Retrieve the cost basis for a specific ticker.

### POST /trade
Insert a new trade.

### GET /subscribe/{ticker}
Subscribe to updates for a specific ticker.

### GET /receive
Receive real-time updates for subscribed tickers.

## API Functionality and Usage

The Trade Tracker API provides the following functionalities:

- Retrieve historical data for a given ticker
- Subscribe to real-time updates for a specific ticker
- Calculate technical indicators such as SMA and RSI
- Manage and track trades
- Calculate cost basis for a specific ticker

To use the API, send HTTP requests to the appropriate endpoints as described above. For example, to retrieve a list of all trades, send a GET request to `/trades`. To insert a new trade, send a POST request to `/trade` with the trade data in the request body.
