package de.fraunhofer.isst.dataspaceconnector.services.messages.handler;

/**
 * This @{@link ArtifactRequestHandler} handles all
 * incoming messages that have a {@link de.fraunhofer.iais.eis.ArtifactRequestMessageImpl} as part
 * one in the multipart message. This header must have the correct '@type' reference as defined in
 * the {@link de.fraunhofer.iais.eis.ArtifactRequestMessageImpl} JsonTypeName annotation.
 */
//@Component
//@SupportedMessageType(ArtifactRequestMessageImpl.class)
//@RequiredArgsConstructor
//public class ArtifactRequestHandler implements MessageHandler<ArtifactRequestMessageImpl> {

//    public static final Logger LOGGER = LoggerFactory.getLogger(ArtifactRequestHandler.class);
//
//    private final @NonNull PolicyEnforcementService pep;
//    private final @NonNull PolicyManagementService pmp;
//    private final @NonNull ResponseMessageService messageService;
//    private final @NonNull ContractAgreementService contractAgreementService;
//    private final @NonNull ConfigurationContainer configurationContainer;
//    private final @NonNull ObjectMapper objectMapper;
//    private final @NonNull ConnectorConfiguration connectorConfiguration;
//
//    private final @NonNull EntityDependencyResolver entityDependencyResolver;
//    private final @NonNull ArtifactService artifactBFFService;
//    private final @NonNull ResourceService<OfferedResource, ?> resourceService;
//    private final @NonNull ContractService contractService;
//    private final @NonNull RuleService ruleService;
//
//    /**
//     * This message implements the logic that is needed to handle the message. As it returns the
//     * input as string the messagePayload-InputStream is converted to a String.
//     *
//     * @param requestMessage The request message
//     * @param messagePayload The message payload
//     * @return The response message
//     * @throws RuntimeException if the response body failed to be build.
//     */
//    @Override
//    // NOTE: Make runtime exception more concrete and add ConnectorConfigurationException, ResourceTypeException
//    public MessageResponse handleMessage(final ArtifactRequestMessageImpl requestMessage, final MessagePayload messagePayload) throws RuntimeException {
//        MessageUtils.checkForEmptyMessage(requestMessage);
//        exceptionService.checkForVersionSupport(requestMessage.getModelVersion());
//
//        // Get a local copy of the current connector.
//        var connector = configurationContainer.getConnector();
//
//        try {
//            // Find artifact and matching resource.
//            final var artifactId = extractArtifactIdFromRequest(requestMessage);
//            final var resource = entityDependencyResolver.findResourceFromArtifactId(artifactId);
//
//            if (resource.isPresent()) {
//                // The resource was not found, reject and inform the requester.
//                LOGGER.debug("Resource could not be found. [id=({}), artifactId=({})]",
//                    requestMessage.getId(), artifactId);
//
//                return ErrorResponse.withDefaultHeader(RejectionReason.NOT_FOUND,
//                    "An artifact with the given uuid is not known to the "
//                        + "connector.",
//                    connector.getId(), connector.getOutboundModelVersion());
//            }
//
//            // Check if the transferred contract matches the requested artifact.
//            if (!checkTransferContract(requestMessage.getTransferContract(),
//                requestMessage.getRequestedArtifact())) {
//                LOGGER.debug("Contract agreement could not be found. [id=({}), contractId=({})]",
//                    requestMessage.getId(), requestMessage.getTransferContract());
//
//                return ErrorResponse.withDefaultHeader(
//                    RejectionReason.BAD_PARAMETERS,
//                    "Missing transfer contract or wrong contract.",
//                    connector.getId(), connector.getOutboundModelVersion());
//            }
//
//            try {
//                // Find the requested resource and its metadata.
//                final var contracts = resource.get().getContracts();
//                final var rules = contractService.get((UUID) contracts.toArray()[0]).getRules();
//                // TODO Should this happen in the backend?
//                final var policy = ruleService.get((UUID)rules.toArray()[0]).getValue();
//
//                try {
//                    // Check if the policy allows data access. TODO: Change to contract agreement. (later)
//                    pep.checkPolicyOnDataProvision(requestMessage.getRequestedArtifact());
//                    Object data;
//                    try {
//                        // Read query parameters from message payload.
//                        QueryInput queryInputData = objectMapper.readValue(IOUtils.toString(
//                                messagePayload.getUnderlyingInputStream(),
//                                StandardCharsets.UTF_8), QueryInput.class);
//                        // Get the data from source.
//                        data = artifactBFFService.getData(artifactId, queryInputData);
//                    } catch (ResourceNotFoundException exception) {
//                        LOGGER.debug("Resource could not be found. "
//                                + "[id=({}), resourceId=({}), artifactId=({}), exception=({})]",
//                            requestMessage.getId(), resource.get().getId(), artifactId,
//                            exception.getMessage());
//                        return ErrorResponse.withDefaultHeader(RejectionReason.NOT_FOUND,
//                            "Resource not found.", connector.getId(),
//                            connector.getOutboundModelVersion());
//                    } catch (InvalidResourceException exception) {
//                        LOGGER.debug("Resource is not in a valid format. "
//                                + "[id=({}), resourceId=({}), artifactId=({}), exception=({})]",
//                            requestMessage.getId(), resource.get().getId(), artifactId,
//                            exception.getMessage());
//                        return ErrorResponse.withDefaultHeader(RejectionReason.INTERNAL_RECIPIENT_ERROR,
//                            "Something went wrong.", connector.getId(),
//                            connector.getOutboundModelVersion());
//                    } catch (ResourceException exception) {
//                        LOGGER.warn("Resource could not be received. "
//                                + "[id=({}), resourceId=({}), artifactId=({}), exception=({})]",
//                            requestMessage.getId(), resource.get().getId(), artifactId,
//                            exception.getMessage());
//                        return ErrorResponse
//                            .withDefaultHeader(RejectionReason.INTERNAL_RECIPIENT_ERROR,
//                                "Something went wrong.", connector.getId(),
//                                connector.getOutboundModelVersion());
//                    } catch (IOException exception) {
//                        LOGGER.debug("Message payload could not be read. [id=({}), " +
//                                        "resourceId=({}), artifactId=({}), exception=({})]",
//                                requestMessage.getId(), resource.get().getId(), artifactId,
//                                exception.getMessage());
//                        return ErrorResponse
//                                .withDefaultHeader(RejectionReason.BAD_PARAMETERS,
//                                        "Malformed payload.", connector.getId(),
//                                        connector.getOutboundModelVersion());
//                    }
//
//                    // Build artifact response.
//                    final var header = messageService.buildArtifactResponseMessage(
//                            requestMessage.getIssuerConnector(),
//                            requestMessage.getTransferContract(),
//                            requestMessage.getId()
//                    );
//                    return BodyResponse.create(header, data);
//                } catch (ConstraintViolationException | MessageException exception) {
//                    // The response could not be constructed.
//                    throw new RuntimeException("Failed to construct the response message.",
//                        exception);
//                } catch (IllegalArgumentException exception) {
//                    LOGGER.warn("Could not deserialize contract. "
//                            + "[id=({}), resourceId=({}), artifactId=({}), exception=({})]",
//                        requestMessage.getId(), resource.get().getId(), artifactId, exception.getMessage());
//                    return ErrorResponse.withDefaultHeader(
//                        RejectionReason.INTERNAL_RECIPIENT_ERROR,
//                        "Policy check failed.",
//                        connector.getId(), connector.getOutboundModelVersion());
//                }
//            } catch (UUIDFormatException exception) {
//                // The resource from the database is not identified via uuids.
//                LOGGER.debug(
//                    "The resource is not valid. The uuid is not valid. [id=({}), exception=({})]",
//                    requestMessage.getId(), exception.getMessage());
//                return ErrorResponse.withDefaultHeader(RejectionReason.NOT_FOUND,
//                    "Resource not found.", connector.getId(),
//                    connector.getOutboundModelVersion());
//            } catch (ResourceNotFoundException exception) {
//                // The resource could be not be found.
//                LOGGER.debug("The resource could not be found. [id=({}), exception=({})]",
//                    requestMessage.getId(), exception.getMessage());
//                return ErrorResponse.withDefaultHeader(RejectionReason.NOT_FOUND,
//                    "Resource not found.", connector.getId(),
//                    connector.getOutboundModelVersion());
//            } catch (InvalidResourceException exception) {
//                // The resource could be not be found.
//                LOGGER.debug("The resource is not valid. [id=({}), exception=({})]",
//                    requestMessage.getId(), exception.getMessage());
//                return ErrorResponse.withDefaultHeader(RejectionReason.NOT_FOUND,
//                    "Resource not found.", connector.getId(),
//                    connector.getOutboundModelVersion());
//            }
//        } catch (UUIDFormatException | RequestFormatException exception) {
//            // No resource uuid could be found in the request, reject the message.
//            LOGGER.debug(
//                "Artifact has no valid uuid. [id=({}), artifactUri=({}), exception=({})]",
//                requestMessage.getId(), requestMessage.getRequestedArtifact(),
//                exception.getMessage());
//            return ErrorResponse.withDefaultHeader(RejectionReason.BAD_PARAMETERS,
//                "No valid resource id found.",
//                connector.getId(),
//                connector.getOutboundModelVersion());
//        } catch (ContractAgreementNotFoundException exception) {
//            LOGGER.warn("Could not load contract from database. "
//                    + "[id=({}), contractId=({}),exception=({})]",
//                requestMessage.getId(), requestMessage.getTransferContract(), exception.getMessage());
//            return ErrorResponse.withDefaultHeader(
//                RejectionReason.BAD_PARAMETERS,
//                "Invalid transfer contract id.",
//                connector.getId(), connector.getOutboundModelVersion());
//        } catch (ContractException exception) {
//            LOGGER.warn("Could not deserialize contract. "
//                    + "[id=({}), contractId=({}),exception=({})]",
//                requestMessage.getId(), requestMessage.getTransferContract(), exception.getMessage());
//            return ErrorResponse.withDefaultHeader(
//                RejectionReason.INTERNAL_RECIPIENT_ERROR,
//                "Something went wrong.",
//                connector.getId(), connector.getOutboundModelVersion());
//        }
//    }
//
//    /**
//     * Extract the artifact id.
//     *
//     * @param requestMessage The artifact request message
//     * @return The artifact id
//     * @throws RequestFormatException if uuid could not be extracted.
//     */
//    private UUID extractArtifactIdFromRequest(final ArtifactRequestMessage requestMessage)
//        throws RequestFormatException {
//        try {
//            // TODO This extraction should be unnecessary. The artifact uri should enough for mapping to endpoint id
//            return UUIDUtils.uuidFromUri(requestMessage.getRequestedArtifact());
//        } catch (UUIDFormatException exception) {
//            throw new RequestFormatException(
//                "The uuid could not extracted from request" + requestMessage.getId(),
//                exception);
//        }
//    }
//
//    /**
//     * Check if the transfer contract is not null and valid.
//     *
//     * @param contractId The id of the contract
//     * @param artifactId The id of the artifact
//     * @return True if everything's fine.
//     */
//    private boolean checkTransferContract(URI contractId, URI artifactId) throws ContractException {
//        final var policyNegotiation = connectorConfiguration.isPolicyNegotiation();
//
//        if (policyNegotiation) {
//            if (contractId == null) {
//                return false;
//            } else {
//                String contractToString;
//                try {
//                    UUID uuid = UUIDUtils.uuidFromUri(contractId);
//                    // Get contract agreement from database.
//                    ResourceContract contract = contractAgreementService.getContract(uuid);
//                    // Get contract from database entry.
//                    contractToString = contract.getContract();
//                } catch (Exception e) {
//                    throw new ContractAgreementNotFoundException("Contract could not be loaded "
//                        + "from database.");
//                }
//
//                Contract agreement;
//                try {
//                    agreement = pmp.getContractAgreement(contractToString);
//                } catch (RequestFormatException exception) {
//                    throw new ContractException("Could not deserialize contract.");
//                }
//
//                URI extractedId = entityDependencyResolver.getArtifactIdFromContract(agreement);
//                return extractedId.equals(artifactId);
//            }
//        } else {
//            return true;
//        }
//    }
//}
