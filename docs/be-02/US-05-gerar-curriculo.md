# US-05 — Gerar currículo do aluno

## Contrato

`GET /api/students/{studentId}/resume` devolve um PDF baseado no perfil persistido:

- Content-Type: application/pdf.
- Content-Disposition: attachment; filename="curriculo-{studentId}.pdf".
- Cache-Control: no-store.
- Arquivo gerado em memória por requisição, sem armazenamento no servidor.

Nome, e-mail, matrícula e curso são necessários. Perfil inexistente retorna 404;
dados mínimos em branco retornam 422 com mensagem clara. Habilidades e experiências
são opcionais: o aluno pode gerar currículo sem experiência profissional.

O documento inclui nome, e-mail, telefone, formação atual, resumo, habilidades,
interesses e experiências. Campos opcionais ausentes e seções vazias são omitidos.
Matrícula é usada para validar a completude, mas não é impressa; senha e demais
dados internos nunca entram no currículo.

## Implementação

StudentResumeService consulta StudentProfileService e gera PDF com Apache PDFBox
3.0.8, versão fixada no pom. StudentProfileController entrega os bytes com os cabeçalhos
de download. A fonte DejaVu Sans está incluída em src/main/resources/fonts, acompanhada
da licença, para não depender das fontes instaladas no sistema.

Há quebra de linhas por largura, tratamento de parágrafos e criação automática de
novas páginas. Acentos são preservados. Caracteres sem glifo na fonte são substituídos
por ?, em vez de impedir a geração. O layout inicial é simples, em português, sem
modelo configurável ou edição independente dos dados do perfil.

## Testes

StudentResumeServiceTest abre os PDFs com PDFBox e extrai texto para verificar nome,
acentos, formação, experiências, período atual, arquivo válido e múltiplas páginas.
Também testa perfil incompleto e texto contendo um caractere não suportado.

StudentProfileControllerTest verifica tipo de conteúdo, nome do arquivo, política
de cache e erros. StudentProfileIntegrationIT verifica que o download usa os dados
persistidos mais recentes e não contém senha, matrícula ou experiências removidas.

## Pendência

A verificação de identidade, papel e acesso ao próprio currículo depende da BE-01.
O identificador na rota é temporário. O critério de autorização da US-05 ainda não
está concluído; não utilizar esse contrato para disponibilizar dados pessoais em produção.
