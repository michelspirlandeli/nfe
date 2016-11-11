package com.fincatto.nfe310.webservices;

import java.math.BigDecimal;
import java.rmi.RemoteException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fincatto.nfe310.NFeConfig;
import com.fincatto.nfe310.classes.NFAutorizador31;
import com.fincatto.nfe310.classes.evento.downloadnf.NFDownloadNFe;
import com.fincatto.nfe310.classes.evento.downloadnf.NFDownloadNFeRetorno;
import com.fincatto.nfe310.transformers.NFRegistryMatcher;
import com.fincatto.nfe310.webservices.downloadnf.NfeDownloadNFStub;
import com.fincatto.nfe310.webservices.downloadnf.NfeDownloadNFStub.NfeCabecMsg;
import com.fincatto.nfe310.webservices.downloadnf.NfeDownloadNFStub.NfeCabecMsgE;
import com.fincatto.nfe310.webservices.downloadnf.NfeDownloadNFStub.NfeDadosMsg;
import com.fincatto.nfe310.webservices.downloadnf.NfeDownloadNFStub.NfeDownloadNFResult;

public class WSDownladNF {

	private static final BigDecimal VERSAO_LEIAUTE = new BigDecimal("1.00");
    private static final String NOME_SERVICO = "DOWNLOAD NFE";
    private final static Logger LOGGER = LoggerFactory.getLogger(WSDownladNF.class);
    private final NFeConfig config;
    
	WSDownladNF (final NFeConfig config) {
        this.config = config;
    }

	public NFDownloadNFeRetorno downloadNotaFiscal(final String cnpj, final String chave) throws Exception {
        final OMElement omElementConsulta = AXIOMUtil.stringToOM(this.gerarDadosDownloadNF(cnpj, chave).toString());
        WSDownladNF.LOGGER.debug(omElementConsulta.toString());
        
        final OMElement omElementRetorno = this.efetuaDownloadNF(omElementConsulta);
        WSDownladNF.LOGGER.debug(omElementRetorno.toString());
        
		return new Persister(new NFRegistryMatcher(), new Format(0)).read(NFDownloadNFeRetorno.class, omElementRetorno.toString());
	}

	private OMElement efetuaDownloadNF(OMElement omElementConsulta) throws RemoteException {
        final NfeCabecMsg cabec = new NfeCabecMsg();
        cabec.setCUF(this.config.getCUF().getCodigoIbge());
        cabec.setVersaoDados(WSDownladNF.VERSAO_LEIAUTE.toPlainString());
        
        final NfeDownloadNFStub.NfeCabecMsgE cabecE = new NfeCabecMsgE();
		cabecE.setNfeCabecMsg(cabec);
        
        final NfeDownloadNFStub.NfeDadosMsg dados = new NfeDadosMsg();
        dados.setExtraElement(omElementConsulta);
		
		NFAutorizador31 autorizador = NFAutorizador31.valueOfCodigoUF(this.config.getCUF());
		final String endpoint = autorizador.getNfeDownloadNF(this.config.getAmbiente());
        if (endpoint == null) {
            throw new IllegalArgumentException("Nao foi possivel encontrar URL para DownloadNF, autorizador " + autorizador.name());
        }
        
		NfeDownloadNFResult nfeDownloadNFResult = new NfeDownloadNFStub(endpoint).nfeDownloadNF(dados, cabecE);
		return nfeDownloadNFResult.getExtraElement();
	}

	private NFDownloadNFe gerarDadosDownloadNF(final String cnpj, final String chave) throws Exception {
		final NFDownloadNFe nfDownloadNFe = new NFDownloadNFe();
		nfDownloadNFe.setVersao(WSDownladNF.VERSAO_LEIAUTE.toPlainString());
		nfDownloadNFe.setAmbiente(this.config.getAmbiente());
		nfDownloadNFe.setServico(WSDownladNF.NOME_SERVICO);
		nfDownloadNFe.setCnpj(cnpj);
		nfDownloadNFe.setChave(chave);
		return nfDownloadNFe;
	}
}
