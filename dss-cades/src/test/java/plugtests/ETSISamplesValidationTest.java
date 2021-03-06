/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package plugtests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.esig.dss.detailedreport.DetailedReport;
import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.test.signature.UnmarshallingTester;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignatureCRLSource;
import eu.europa.esig.dss.validation.SignatureCertificateSource;
import eu.europa.esig.dss.validation.SignatureOCSPSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;

/**
 * This test is only to ensure that we don't have exception with valid? files
 */
public class ETSISamplesValidationTest {

	private static final List<String> DETACHED_SIGNATURES = Arrays.asList("Signature-C-SK-20.p7s", "Signature-C-SK-59.p7s");

	public static Stream<Arguments> data() {
		File folder = new File("src/test/resources/plugtest");
		Collection<File> listFiles = Utils.listFiles(folder, new String[] { "p7", "p7b", "p7m", "p7s", "pkcs7", "csig" }, true);
		Collection<Arguments> dataToRun = new ArrayList<>();
		for (File file : listFiles) {
			dataToRun.add(Arguments.of( file ));
		}
		return dataToRun.stream();
	}

	@ParameterizedTest(name = "Validation {index} : {0}")
	@MethodSource("data")
	public void testValidate(File fileToTest) {
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(new FileDocument(fileToTest));
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setIncludeCertificateTokenValues(true);
		certificateVerifier.setDataLoader(new IgnoreDataLoader());
		validator.setCertificateVerifier(certificateVerifier);

		if (DETACHED_SIGNATURES.contains(fileToTest.getName())) {
			validator.setDetachedContents(Arrays.asList(new InMemoryDocument("Hello world".getBytes(StandardCharsets.UTF_8))));
		}

		Reports reports = validator.validateDocument();
		assertNotNull(reports);

		DiagnosticData diagnosticData = reports.getDiagnosticData();
		assertNotNull(diagnosticData);

		List<CertificateWrapper> usedCertificates = diagnosticData.getUsedCertificates();
		for (CertificateWrapper certificateWrapper : usedCertificates) {
			byte[] binaries = certificateWrapper.getBinaries();
			assertNotNull(DSSUtils.loadCertificate(binaries));
		}

		SimpleReport simpleReport = reports.getSimpleReport();
		assertNotNull(simpleReport);

		DetailedReport detailedReport = reports.getDetailedReport();
		assertNotNull(detailedReport);

		List<AdvancedSignature> signatures = validator.getSignatures();
		for (AdvancedSignature advancedSignature : signatures) {
			assertNotNull(advancedSignature);
			SignatureCertificateSource certificateSource = advancedSignature.getCertificateSource();
			assertNotNull(certificateSource);

			assertNotNull(certificateSource.getKeyInfoCertificates());
			assertNotNull(certificateSource.getSigningCertificateValues());
			assertNotNull(certificateSource.getCertificateValues());
			assertNotNull(certificateSource.getCompleteCertificateRefs());
			assertNotNull(certificateSource.getAttributeCertificateRefs());
			assertNotNull(certificateSource.getTimeStampValidationDataCertValues());
			assertTrue(certificateSource.getDSSDictionaryCertValues().isEmpty());
			assertTrue(certificateSource.getVRIDictionaryCertValues().isEmpty());

			SignatureCRLSource crlSource = advancedSignature.getCRLSource();
			assertNotNull(crlSource);
			assertTrue(Utils.isCollectionEmpty(crlSource.getTimestampSignedDataRevocationTokens()));
			assertTrue(Utils.isCollectionEmpty(crlSource.getTimestampRevocationValuesTokens()));
			assertTrue(Utils.isCollectionEmpty(crlSource.getTimestampRevocationRefs()));
			
			SignatureOCSPSource ocspSource = advancedSignature.getOCSPSource();
			assertNotNull(ocspSource);
			assertTrue(Utils.isCollectionEmpty(ocspSource.getTimestampSignedDataRevocationTokens()));
			assertTrue(Utils.isCollectionEmpty(ocspSource.getTimestampRevocationValuesTokens()));
			assertTrue(Utils.isCollectionEmpty(ocspSource.getTimestampRevocationRefs()));
		}

		UnmarshallingTester.unmarshallXmlReports(reports);
	}

}
