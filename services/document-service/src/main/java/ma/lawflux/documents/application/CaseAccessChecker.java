package ma.lawflux.documents.application;

import java.util.UUID;

/**
 * À implémenter avec l'accès aux dossiers du chef (case-service, CheckCaseAccess).
 * Doit renvoyer false si le dossier n'existe pas OU n'appartient pas au cabinet.
 */
public interface CaseAccessChecker {
    boolean canAccess(UUID cabinetId, UUID caseId);

    /** Action reconnue par CheckCaseAccess ; refus sûr si le service est indisponible. */
    default boolean canAccess(UUID cabinetId, UUID caseId, String action) {
        return canAccess(cabinetId, caseId);
    }
}
