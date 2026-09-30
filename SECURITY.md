# Security Policy

## Supported versions

The main branch is the supported development and release line. Older commits and experimental branches are not guaranteed to receive security fixes.

## Private vulnerability reporting

Please report security vulnerabilities privately through **GitHub Private Security Advisories** for this repository. Do not create a public issue for an unpatched vulnerability.

If GitHub Private Security Advisories are unavailable, contact the repository maintainer privately through the contact mechanism listed on the maintainer's GitHub profile.

Do not include farmer names, phone numbers, exact locations, crop photos, voice recordings, API keys, or other sensitive data in a report unless strictly necessary.

## Response targets

- Acknowledge a valid private report within 3 business days.
- Triage severity and affected versions within 7 business days.
- Provide a remediation plan or mitigation for confirmed high/critical issues as soon as practical.
- Coordinate disclosure timing with the reporter after a fix is available.

These are response targets, not contractual guarantees.

## Agricultural safety

Report unsafe agricultural recommendations, hallucinated claims, dangerous treatment instructions, or other issues that could cause harm through the same private channel whenever possible.

## Secrets

Never commit real API keys, tokens, passwords, service-account files, private keys, or production connection strings.

Production secrets belong in Render environment variables or the appropriate provider secret store.

If a real credential is ever committed, rotate/revoke it immediately at the provider and investigate the Git history.

## Secret scanning

The repository includes Gitleaks CI on every push and pull request. Maintainers should also enable GitHub Secret Scanning and Push Protection in repository Settings -> Code security and analysis.
