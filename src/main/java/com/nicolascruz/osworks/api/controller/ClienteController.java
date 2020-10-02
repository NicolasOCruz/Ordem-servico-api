package com.nicolascruz.osworks.api.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicolascruz.osworks.api.model.ClienteInput;
import com.nicolascruz.osworks.api.model.ClienteModel;
import com.nicolascruz.osworks.domain.model.Cidade;
import com.nicolascruz.osworks.domain.model.Cliente;
import com.nicolascruz.osworks.domain.model.Endereco;
import com.nicolascruz.osworks.domain.model.TipoCliente;
import com.nicolascruz.osworks.domain.repository.CidadeRepository;
import com.nicolascruz.osworks.domain.repository.ClienteRepository;
import com.nicolascruz.osworks.domain.service.CadastroClienteService;
import com.nicolascruz.osworks.domain.service.PageClienteService;
import com.nicolascruz.osworks.domain.service.exceptions.DataIntegrityException;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/clientes")
public class ClienteController {

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private CadastroClienteService cadastroCliente;

	@Autowired
	private PageClienteService paginaCliente;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private CidadeRepository cidadeRepository;

	@GetMapping
	public List<ClienteModel> listar() {
		return toCollectionModel(clienteRepository.findAll());
	}

	@GetMapping("/{clienteId}")
	public ResponseEntity<ClienteModel> buscar(@PathVariable Long clienteId) { // anotação para fazer o Binding no
																				// {clientesId}
		Optional<Cliente> cliente = clienteRepository.findById(clienteId); // Optional é um container onde pode ter algo
																			// dentro ou não

		if (cliente.isPresent()) {

			ClienteModel clienteModel = toModel(cliente.get());

			return ResponseEntity.ok(clienteModel);
			// se tiver algo dentro do conteiner, retorne uma Response 200 (ok)
		}
		// return cliente.orElse(null); //se nao tiver nada dentro do container retorna
		// null

		// caso contrario, retorne uma Response 404 (not found)
		return ResponseEntity.notFound().build();
	}

	@GetMapping("/cpf/{clienteCpf}")
	public ResponseEntity<ClienteModel> buscarCpf(@PathVariable String clienteCpf) {
		Cliente cliente = clienteRepository.findByCpf(clienteCpf);

		if (cliente != null) {

			ClienteModel clienteModel = toModel(cliente);
			return ResponseEntity.ok(clienteModel);

		}
		return ResponseEntity.notFound().build();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED) // retorna o status 201, poderia fazer com response entity tbm
	public ClienteModel adicionar(@Valid @RequestBody ClienteInput clienteInput) { // tranforme o JSON do corpo da
																					// requisição em um objeto

		Cliente cliente = fromDTO(clienteInput);
		System.out.println(cliente.toString());
		return toModel(cadastroCliente.salvar(cliente));
	}

	@PutMapping("/{clienteId}")
	public ResponseEntity<ClienteModel> atualizar(@Valid @PathVariable Long clienteId,
			@RequestBody ClienteInput clienteInput) {

		if (!clienteRepository.existsById(clienteId)) {
			return ResponseEntity.notFound().build();
		}
		// tem que setar o ID, porque se passar um parametro sem ID ele vai criar um
		// cliente novo
		Cliente cliente = fromDTO(clienteInput);
		cliente.setId(clienteId);
		cliente = cadastroCliente.update(cliente);

		ClienteModel clienteModel = toModel(cliente);

		return ResponseEntity.ok(clienteModel);
	}

	@DeleteMapping("/{clienteId}")
	public ResponseEntity<Void> remover(@PathVariable Long clienteId) {
		if (!clienteRepository.existsById(clienteId)) {
			return ResponseEntity.notFound().build();
		}
		try {
			cadastroCliente.excluir(clienteId);
			return ResponseEntity.noContent().build();
		} catch (DataIntegrityViolationException e) {
			throw new DataIntegrityException("Há ordens de serviço vinculadas a esse cliente");
		}

	}

	@GetMapping("/page")
	public ResponseEntity<Page<ClienteModel>> findPage(
			// Atributos opcionais na rewuisição, com valores padrões caso não sejam
			// informados
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "linesPerPage", defaultValue = "24") Integer linesPerPage,
			@RequestParam(value = "orderBy", defaultValue = "nome") String orderBy,
			@RequestParam(value = "direction", defaultValue = "ASC") String direction) {

		Page<Cliente> listCliente = paginaCliente.findPage(page, linesPerPage, orderBy, direction);

		Page<ClienteModel> listClienteModel = listCliente.map(obj -> new ClienteModel(obj));

		return ResponseEntity.ok().body(listClienteModel);
	}

	private ClienteModel toModel(Cliente cliente) {
		return modelMapper.map(cliente, ClienteModel.class);
	}

	private List<ClienteModel> toCollectionModel(List<Cliente> clientes) {
		return clientes.stream() // strem retorna um fluxo de elementos que suportam operacoes de
									// agregacao/tranforacao
				.map(cliente -> toModel(cliente)) // map vai aplicar uma função a cada elemento um a um do stream e
													// retornar um novo stream como resultado
				.collect(Collectors.toList()); // vai reduzir o stream anterior para uma coleção
	}

	/*private Cliente toEntity(ClienteInput clienteInput) {
		return modelMapper.map(clienteInput, Cliente.class);
	}*/

	private Cliente fromDTO(ClienteInput cliente) {
		Cliente cli = new Cliente(null, cliente.getNome(), cliente.getEmail(), cliente.getTelefone(), cliente.getCpf(),
				TipoCliente.toEnum(cliente.getTipo()));
		Cidade city = cidadeRepository.findById(cliente.getCidadeId()).orElse(null);

		Endereco end = new Endereco(null, cliente.getLogradouro(), cliente.getNumero(), cliente.getComplemento(),
				cliente.getBairro(), cliente.getCep(), cli, city);
		cli.getEnderecos().add(end);
		return cli;
	}
}