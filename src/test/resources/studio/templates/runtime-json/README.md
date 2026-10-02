# Core Ikasan topology fixtures

`simpleFlow.json` and `multiRecipientFlow.json` are copied unchanged from core Ikasan's `ikasaneip/topology/src/test/resources/data/` in the approved Ikasan 4 reference checkout. They exercise the actual module metadata contract, including consumer duplication in `flowElements` and reverse-ordered transitions. The original Ikasan project is copyright Ikasan Enterprise Integration Platform and uses the BSD 3-Clause licence; see the repository's `LICENSE.txt`.

The tests wrap each flow in a module document and check import with both supported Studio packs. These fixture classes are examples of external implementations, not application code supplied by Studio.
