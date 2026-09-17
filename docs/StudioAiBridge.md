# AI proposals against the live Studio model

Studio can expose the open project's in-memory model to an MCP client. The client reads a
snapshot and the selected meta-pack's catalogue, then submits operations for review. It does
not edit `model.json` or apply changes itself.

## Connect

1. Open and configure Ikasan Studio. Finish any pending property edits and generation.
2. Select **Tools → Connect AI to Ikasan Studio…** (also available through Find Action).
3. Copy the configuration into your AI client's MCP server settings. The bundled stdio adapter
   requires Python 3; change `python3` to your Python executable if necessary.
4. Ask the AI to read `studio_snapshot` and `studio_catalogue` before suggesting changes.
5. Review the proposed operations in Studio and select **Apply** or **Cancel**.

The configuration is specific to this project and running IDE session. Closing the connection
dialog keeps the bridge running. Reopen the action and select **Stop bridge** to disconnect.
Closing the project also stops it. After starting a new bridge, copy its new configuration.

The adapter uses [MCP stdio](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports).
Its private HTTP connection is bound to `127.0.0.1`, requires a random bearer token, rejects
browser Origin headers, and is not a public Streamable HTTP MCP endpoint. Connection files live
in IntelliJ's system directory, outside the project; on POSIX systems the directory and token
file are owner-only. Do not share the connection file. No AI account, model provider or cloud
service is bundled. Known password, secret, token and credential fields are redacted from
snapshots; other model content is visible to the connected client, including text properties
that might themselves contain sensitive data.

## Tools

| Tool | Result |
| --- | --- |
| `studio_snapshot` | Live model, project name and opaque revision. No disk reload. |
| `studio_catalogue` | Component keys, property types/defaults/choices and payload contracts for the selected meta-pack. |
| `studio_propose` | Validates operations on an isolated model and opens a review. Returns a proposal ID and summary. |
| `studio_proposal_status` | `awaiting_review`, `cancelled`, `generating`, `applied`, `generation_failed`, or `undone`. |

Snapshots are bounded to the last eight reads and statuses to the last sixteen proposals.
Only one review can be pending per project. A proposal is checked again immediately before
Apply. Manual edits, model reloads, version migration and pending property edits prevent a
stale proposal from being applied. Obtain a fresh snapshot and propose again.

## Operation contract

Submit `{"revision":"<from studio_snapshot>","operations":[...]}` to `studio_propose`.
Operations execute in array order on a detached candidate. Up to 100 operations are accepted.

- `addFlow`: `type`, `flow` (new flow name).
- `addComponent`: `type`, `flow`, `key` (exact catalogue key), `name`, optional `properties` object.
  A consumer occupies the flow's consumer slot; other components are inserted before its terminal
  producer. Required properties with metadata defaults are materialised as in Studio.
- `setProperty`: `type`, `flow`, `component` (existing name), `property`, `value` (scalar or null).
- `connect`: `type`, `flow`, `order` (every component name exactly once, consumer first).
  This defines a linear flow order. Studio derives its persisted transitions.

For example, with an FTP-capable selected meta-pack:

```json
{
  "revision": "<revision returned by studio_snapshot>",
  "operations": [
    {"type": "addFlow", "flow": "Transfer"},
    {"type": "addComponent", "flow": "Transfer", "key": "FTP Consumer", "name": "ReadFiles",
     "properties": {"cronExpression": "0/5 * * * * ?", "sourceDirectory": "/incoming"}},
    {"type": "addComponent", "flow": "Transfer", "key": "FTP Producer", "name": "WriteFiles",
     "properties": {"outputDirectory": "/outgoing"}},
    {"type": "connect", "flow": "Transfer", "order": ["ReadFiles", "WriteFiles"]}
  ]
}
```

Read the catalogue for required host, port and credential settings; the example uses metadata
defaults where available. Submit complete valid flows. Unsupported components/properties, duplicate
names, missing required values, invalid property types/choices and known payload mismatches reject
the whole proposal. Payload checking uses Studio's design-time metadata and is not a substitute
for compiling and testing the generated application.

The initial API supports linear flows. Routers, edits to branched flows, exception resolvers,
deletion, renaming, implementation-class changes and version migration must use Studio's existing UI.
Unrelated flows and their object identities are preserved. No operation grants permission to
overwrite developer-owned code.

## Apply, generation and undo

Apply updates the live model, refreshes the canvas/properties, and uses Studio's normal protected
model persistence and generation pipeline. The model proposal has one global IntelliJ undo entry;
Undo and Redo regenerate the corresponding output. A failed immediate save rolls back the model.
An asynchronous generation failure is reported by Studio and as `generation_failed`: the model
remains applied so the developer can fix the problem and regenerate or undo it.

Keep the offline contract for workflows with Studio closed. Never modify the model file underneath
an open Studio session as a substitute for the live bridge.
