```Gherkin
Feature: Sanitize Baidu search page

Scenario: Carry out a Baidu search
    Given the search query URL is like: https://www.baidu.com/s?wd=whatever+you+want+to+know
    When the page loads
    Then all irrelevant contents on the page should be removed
```
