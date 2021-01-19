# statuspage-gating

[![Build Status](https://ci.jenkins.io/job/Plugins/job/statuspage-gating-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/statuspage-gating-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/statuspage-gating-plugin.svg)](https://github.com/jenkinsci/statuspage-gating-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/statuspage-gating.svg)](https://plugins.jenkins.io/statuspage-gating)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/statuspage-gating-plugin.svg?label=changelog)](https://github.com/jenkinsci/statuspage-gating-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/statuspage-gating.svg?color=blue)](https://plugins.jenkins.io/statuspage-gating)

## Introduction

Fetch uptime matrices from [StatusPage](https://www.atlassian.com/software/statuspage) for
[Jenkins gating](https://github.com/jenkinsci/gating-core-plugin) to use.

## Getting started

- Configure connection to StatusPage:

```yaml
unclassified:
  statuspageGating:
    sources:
      - label: "StatusPage" 
        apiKey: "foobar" # Encrypt this in production setting. Optional field
        url: "https://acme.com" # Only when different from https://api.statuspage.io/v1/
        pages: [ "foo", "bar" ]

```
(Or manually on global config page)

- See what matrices are available to Jenkins at JENKINS_URL/gating/.
- configure your builds to [wait for your infra](https://github.com/jenkinsci/gating-core-plugin).

## Useful links

- https://developer.statuspage.io/
- https://support.atlassian.com/statuspage/resources/

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

