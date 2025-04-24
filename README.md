# HobbyInvestor Stock: Stock Reader

This is a standalone Spring Boot scheduled task that reads the following types of data from the Finnhub Stock API:
1. Stock Symbols
2. Stock Symbol Financials
3. Stock Symbol Prices (Time Series)

This process runs separate cron schedules for each category of data.

The targets for each data category is as follows:

| Data Category           | Target                               |
|-------------------------|--------------------------------------|
| Stock Symbols           | Redis cache (StockSymbol)            |
| Stock Symbol Financials | Kafka topic (companyFinancialsUpdate) |
| Stock Symbol News       | Kafka topic (companyNewsUpdate)      |

Example Redis Commands:
1. SMEMBERS StockSymbol
2. HGETALL StockSymbol:CHLE