/**
 * The psi package is the interface into Intellij's Program Structure Interface
 *
 * We don't want any Ikasan model state stored here, its only purpose should be to extract information from
 * Intellij to populate models in the other packages, or to take new changes from the UI and update the underlying code.
 *
 */
package org.ikasan.studio.model.psi;