package org.isatools.isacreator.ontologiser.adaptors;

import org.isatools.isacreator.apiutils.InvestigationUtils;
import org.isatools.isacreator.apiutils.SpreadsheetUtils;
import org.isatools.isacreator.configuration.Ontology;
import org.isatools.isacreator.model.Assay;
import org.isatools.isacreator.model.Investigation;
import org.isatools.isacreator.model.Study;
import org.isatools.isacreator.ontologiser.model.OntologisedResult;
import org.isatools.isacreator.ontologymanager.OntologySourceRefObject;
import org.isatools.isacreator.ontologymanager.bioportal.model.BioPortalOntology;
import org.isatools.isacreator.ontologyselectiontool.OntologyObject;
import org.isatools.isacreator.ontologyselectiontool.OntologySourceManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 03/02/2011
 *         Time: 22:43
 */
public class InvestigationAdaptor implements ContentAdaptor {

    private Investigation investigation;

    // by creating and maintaining this Map, we are able to locate which Spreadsheets
    // contain which terms, making string substitution much quicker.

    private Map<Assay, Set<String>> assayToTerms;

    public InvestigationAdaptor(Investigation investigation) {
        this.investigation = investigation;
        assayToTerms = new HashMap<Assay, Set<String>>();
    }

    public void replaceTerms(Set<OntologisedResult> annotations) {

        Map<String, OntologyObject> mappingsForReplacement = new HashMap<String, OntologyObject>();

        // for each annotation, if it has an ontology selected, use that and replace the values in the spreadsheet.

        for (OntologisedResult annotation : annotations) {

            if (annotation.getAssignedOntology() != null) {

                Ontology sourceOntology = annotation.getAssignedOntology().getOntologySource();

                // adding ontology source in case it has not already been added
                OntologySourceManager.addToUsedOntologySources(investigation.getInvestigationId(),
                        new OntologySourceRefObject(sourceOntology.getOntologyAbbreviation(), "",
                                sourceOntology.getOntologyVersion(), sourceOntology.getOntologyDisplayLabel()));

                BioPortalOntology ontology = annotation.getAssignedOntology().getOntologyTerm();

                // add the term to the ontology history.
                OntologyObject ontologyObject = new OntologyObject(ontology.getOntologyTermName(), ontology.getOntologySourceAccession(), ontology.getOntologySource());

                mappingsForReplacement.put(annotation.getFreeTextTerm(), ontologyObject);

                OntologySourceManager.addToUserHistory(ontologyObject);
            }
        }

        // now replace the terms in each of the Spreadsheets available within ISAcreator
        for (String studyAccession : investigation.getStudies().keySet()) {
            Study study = investigation.getStudies().get(studyAccession);

            System.out.println("Replacing terms in " + studyAccession);
            SpreadsheetUtils.replaceFreeTextWithOntologyTerms(study.getStudySample().getSpreadsheetUI().getTable(), mappingsForReplacement);

            for (Assay assay : study.getAssays().values()) {
                System.out.println("Replacing terms in " + assay.getAssayReference());
                SpreadsheetUtils.replaceFreeTextWithOntologyTerms(assay.getSpreadsheetUI().getTable(), mappingsForReplacement);
            }
        }
    }

    public Set<String> getTerms() {
        Map<Assay, Map<String, Set<String>>> result = InvestigationUtils.getFreeTextInInvestigationSpreadsheets(investigation);

        return createFlattenedSet(result);
    }

    private Set<String> createFlattenedSet(Map<Assay, Map<String, Set<String>>> toFlatten) {

        Set<String> flattenedSet = new HashSet<String>();

        for (Assay assay : toFlatten.keySet()) {
            Set<String> assayTerms = new HashSet<String>();
            for (String columnName : toFlatten.get(assay).keySet()) {
                assayTerms.addAll(toFlatten.get(assay).get(columnName));

            }
            flattenedSet.addAll(assayTerms);
            assayToTerms.put(assay, assayTerms);
        }

        return flattenedSet;
    }
}
