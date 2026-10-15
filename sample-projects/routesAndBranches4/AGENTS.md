# Ikasan Studio agent instructions

This project is managed by Ikasan Studio. Before editing `generated/src/main/model/model.json`,
read `generated/IKASAN_STUDIO.md` and
`generated/src/main/model/component-catalogue.json`.

`model.json` is the version-neutral source of truth. Make minimal changes, preserve unknown
fields, validate the result, and let Ikasan Studio regenerate its owned files. Do not directly
edit files under `generated/` other than `model.json`. Developer-owned implementations belong
under `user/` and must never be overwritten without explicit permission.
