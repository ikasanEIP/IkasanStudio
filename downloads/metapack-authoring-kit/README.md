# Meta-pack authoring kit downloads

Download **both** ZIP files from this directory using GitHub's **Download raw file** button:

1. `ikasan-metapack-authoring-kit-part1.zip`
2. `ikasan-metapack-authoring-kit-part2.zip`

These are two ordinary ZIP archives containing complementary files, not a multipart ZIP
that needs joining. Extract **both into the same parent directory**. Their contents combine
into one `ikasan-metapack-authoring-kit/` directory. Neither archive is sufficient alone.
On Windows, select the same destination for both extractions rather than accepting separate
part1/part2 destination folders. Do not concatenate the files.

With Python 3.12 on the VDI, these commands provide an unambiguous extraction:

```text
python -m zipfile -e ikasan-metapack-authoring-kit-part1.zip .
python -m zipfile -e ikasan-metapack-authoring-kit-part2.zip .
cd ikasan-metapack-authoring-kit
python verify.py
```

Use `python3` or `py -3` instead of `python` if that is your Python launcher. Verification
requires JDK 17 and Maven 3.8.6+ (3.x); see the extracted README for Maven path options.
The verifier checks the complete original inventory, so a missing second ZIP is detected.
`SHA256SUMS.txt` contains the checksums of the two downloaded archives.

The pair restores the previously verified public authoring kit byte-for-byte. It includes
the development plugin captured when that kit was built; it is not automatically refreshed
when a newer Studio plugin is built. The maintained kit sources live in
[`tools/metapack-authoring-kit`](../../tools/metapack-authoring-kit/README.md).

Each archive is below GitHub's 100 MiB regular Git file limit. Generated download copies
are intentionally committed here for restricted workstations; editable sources remain
under `tools/`, and normal generated build output remains under `build/`.
