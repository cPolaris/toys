### f2b_log.py

```Gherkin
Feature: Automatically publish fail2ban log info to a Gist from a remote server that I cannot access behind a firewall.

Scenario: Some time has elapsed
    Given my remote server is still running
    And my Gist is still there
    When the script finishes running
    Then a new row of log data should appear in the gist
```

### submit.py

```Gherkin
Feature: programmatically submit homework files to the school server.

Scenario: Handing in a lab assignment
    Given the school submission webpage has not been modified
    And the script is used correctly
    When the correct commands are typed in
    Then it should output a success message obtained from school server
    And my assignment should have been uploaded to the school server
```

### rename_and_recode_txt.sh

```Gherkin
Feature: Mass rename and re-encode some text files at my disposal
```
