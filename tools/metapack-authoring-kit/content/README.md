# Ikasan shared-component meta-pack — VDI authoring kit

Start here. This kit contains public material only. It creates a complete **SharedIkasan4** pack
for **Ikasan 4.1.6**, using **JDK 17**. You do not need to clone/build Studio, Ikasan or Dashboard.

## First use

1. Extract the ZIP into an approved writable work folder (use a short path, such as C:\ikasan-kit on Windows). Keep a pristine copy of the original ZIP.
2. Install/use approved Python 3.10+, JDK 17 and Maven 3.8.6 or newer Maven 3.x. Set JAVA_HOME and PATH.
3. From the extracted kit root run:

   ```sh
   python verify.py --maven /path/to/mvn
   ```

   On Windows use `py -3 verify.py --maven "C:\path\to\maven\bin\mvn.cmd"` if that is your
   approved Python launcher. The scripts use paths rather than shell interpolation. If your Python
   cannot launch the Maven command wrapper, use the equivalent Maven command below in your normal
   terminal. Set JAVA_HOME to your JDK 17; the Maven path alone does not choose Java.

4. The verifier checks the public SHA-256 inventory, copies the bundled repository into the writable
   `work/maven-repository`, then runs `clean install` **offline** with neutral settings. It validates
   descriptors/templates, round-trips a model, compiles the generated factory and validates the pack JAR.
5. If included, install the ZIP under `plugin/` using IntelliJ **Settings → Plugins → gear → Install Plugin
   from Disk**, then restart. This is a local development build; see `build-receipt.json` for provenance.
6. Use **Tools → Ikasan Studio → Install Meta-Pack…** to install
   `starter/example-pack/target/example-pack-0.1.0-SNAPSHOT.jar`, then restart. The equivalent prebuilt
   public example is also included at `samples/shared-ikasan4-example-pack.jar`.
7. Select `SharedIkasan4` in a scratch Studio project. The built public model example is at
   `starter/example-pack/target/samples/model.json`. Its flow uses standard scheduling and three public
   example components. No private/shared implementation is included yet.

Keep `dependency-repository` pristine. Subsequent verification may rebuild `starter/**/target` and
install sample runtime artifacts into `work/maven-repository`; it does not modify the original
repository or public inventory. Configure IntelliJ's Maven local repository to the full path of
`work/maven-repository` when importing the starter or resolving its example runtime in a sample app.
The application's own additional Maven dependencies may require your approved internal repository.

The equivalent command, from `starter/` with an already prepared work repository, is:

```sh
mvn -B -o -nsu -s ../settings.xml -gs ../settings.xml -Dmaven.repo.local=../work/maven-repository -Djava.awt.headless=true clean install
```

Use an absolute repository path if your IDE changes the working directory. Do not remove `-o` from
the neutral-settings command to contact internal repositories: configure an approved separate Maven
profile/settings file on the VDI for intentional dependency provisioning. Never put its credentials
into this kit or send it back outside work.

## Add the real shared components

Open `starter/` in your approved IDE. Its `.github/copilot-instructions.md` gives Copilot the pack
contract. Read `docs/WORKFLOW.md` and use the numbered prompts under `.github/prompts/`; they can be
copied into Chat if your Copilot client does not support prompt-file invocation. Start with one
component. Copilot may inspect private code only if that usage is approved by your organisation.
The IDE location does not imply that AI inference or code context remains on the VDI.

After intentional source/descriptor changes, use:

```sh
python verify.py --build-only --maven /path/to/mvn
```

`--build-only` skips public starter checksums because you deliberately changed the starter; Maven
still runs offline. Provide newly required private dependencies/plugins through your approved work
repository/cache first. Keep the build receipt/inventory from the pristine kit as a baseline record.
Run `python verify.py --inventory-only` to check the original public kit before editing it.

The starter retains example Maven coordinates and three example classes. Choose internal coordinates,
a stable pack ID and a released pack revision before production adoption; update POMs, descriptors,
manifest, Java tests and archive filenames together. Replace example runtime dependencies with your
actual library coordinates. Installing a resource-only pack does not provision application libraries.

## Contents

- `starter/`: maintained Maven example adapted to the distinct pack ID `SharedIkasan4`.
- `samples/`: prebuilt public example pack, its separate runtime JAR and its example model.
- `reference/`, `schemas/`, `public-api-source/`: public contract and engine/test-kit context.
  Reference documents retain repository-relative links; use the flat reference filenames locally.
- `.github/`: Copilot rules and staged prompts, also copied into `starter/.github/`.
- `docs/`: workflow, inventory worksheets, generation rules, acceptance and troubleshooting.
- `dependency-repository/`: curated baseline and build/test dependencies; never a copy of user settings.
- `plugin/`: matching development plugin if requested during assembly.
- `inventory.json`, `build-receipt.json`: public checksums, baseline and verification receipt.

Keep the resulting private pack, inventories, generated examples and test reports inside work.
No private source, pack, configuration export or screenshots are needed by the external Studio maintainer.
