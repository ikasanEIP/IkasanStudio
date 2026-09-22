# MigrationRegression

Open this Maven project in IntelliJ with Ikasan Studio installed. The baseline model
is `generated/src/main/model/model.json`; custom implementations live in `user/`.

The reusable verification suite is in `verification/`. The included controller works from this project directory:

```
python3 fixture.py serve .
python3 fixture.py verify .
```

Start services before running the module manually. Automated verification starts its
own isolated services and application. It does not test or stop your currently running
IDE application. See the fixture README in the Studio repository for migration and
before/after comparison commands. Do not edit generated sources to make a test pass.
