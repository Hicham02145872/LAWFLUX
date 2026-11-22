package ma.lawflux.documents.application;

/**
 * À implémenter par le contexte de sécurité commun du chef.
 * Doit lever une StatusRuntimeException UNAUTHENTICATED si le jeton est absent ou invalide.
 */
public interface CallerContextProvider {
    CallerContext current();
}