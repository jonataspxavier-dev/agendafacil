# Projeto 2 — Sistema de Agendamentos "AgendaFácil" (React + Spring Boot + PostgreSQL)

> Plano para rodar no Claude Code. Cole este arquivo na raiz de uma pasta vazia como `PLANO.md` e siga as fases em ordem.

## Objetivo

Construir um sistema full stack completo de agendamento de serviços (para salões, clínicas, consultórios), com painel administrativo e área do cliente. Este é o projeto "carro-chefe" do portfólio: mostra front, back, banco, autenticação e Docker em um só lugar.

**Tempo estimado:** 5 a 8 dias
**Stack:**
- Back-end: Java 21, Spring Boot 3.x (Web, Data JPA, Security, Validation), JWT, Flyway, PostgreSQL
- Front-end: React + Vite + TypeScript, React Router, Axios, React Hook Form + Zod, Tailwind CSS
- Infra: Docker e Docker Compose; testes com JUnit 5, Mockito e Testcontainers
- Docs: Swagger/OpenAPI (springdoc)

## Perfis de usuário

- **ADMIN:** cadastra serviços, profissionais e horários; vê todos os agendamentos e o dashboard
- **CLIENTE:** cria conta, vê horários livres, agenda, cancela e vê o próprio histórico

## Funcionalidades (MVP)

1. Cadastro e login com JWT (access token + refresh token), senhas com BCrypt
2. CRUD de **Serviços** (nome, descrição, duração em minutos, preço, ativo)
3. CRUD de **Profissionais** (nome, especialidades, dias e horários de trabalho)
4. **Cálculo de horários disponíveis** por profissional, serviço e data, sem permitir conflitos
5. **Agendamento**: criar, cancelar (até 2h antes), marcar como concluído
6. **Dashboard admin**: agendamentos do dia, faturamento do mês, serviços mais vendidos
7. Paginação, filtros e ordenação nas listagens
8. Tratamento global de erros com respostas padronizadas
9. Seed com dados de exemplo para a demonstração

## Modelo de dados

```
usuario(id, nome, email UNIQUE, senha_hash, perfil[ADMIN|CLIENTE], criado_em)
servico(id, nome, descricao, duracao_min, preco, ativo)
profissional(id, nome, ativo)
profissional_servico(profissional_id, servico_id)
jornada(id, profissional_id, dia_semana, hora_inicio, hora_fim)
agendamento(id, cliente_id, profissional_id, servico_id, inicio, fim,
            status[AGENDADO|CANCELADO|CONCLUIDO], preco_cobrado, criado_em)
```

## Endpoints principais

```
POST   /api/auth/register | /login | /refresh
GET    /api/servicos                      (público)
POST   /api/servicos                      (ADMIN)  PUT/DELETE /api/servicos/{id}
GET    /api/profissionais                 (público)
POST   /api/profissionais                 (ADMIN)  PUT/DELETE /api/profissionais/{id}
GET    /api/disponibilidade?profissionalId=&servicoId=&data=
POST   /api/agendamentos                  (CLIENTE)
GET    /api/agendamentos/meus             (CLIENTE)
PATCH  /api/agendamentos/{id}/cancelar
PATCH  /api/agendamentos/{id}/concluir    (ADMIN)
GET    /api/admin/agendamentos?data=&status=&page=
GET    /api/admin/dashboard
```

## Estrutura de pastas

```
agendafacil/
├── backend/            # Spring Boot (controller, service, repository, dto, entity, security, exception)
├── frontend/           # React + Vite (pages, components, services, hooks, contexts)
├── docker-compose.yml  # postgres + backend + frontend
├── README.md
└── PLANO.md
```

## Fases — prompts para colar no Claude Code

**Fase 0 — Setup**
```
Leia o PLANO.md. Crie um CLAUDE.md com as convenções (camadas do Spring, DTOs com records, nunca expor entidades na API, testes obrigatórios para regras de negócio, commits pequenos). Gere o projeto Spring Boot em /backend e o React+Vite+TS em /frontend, e um docker-compose com PostgreSQL. Confirme que tudo sobe antes de seguir.
```

**Fase 1 — Banco e entidades**
```
Crie as migrations Flyway para o modelo de dados do PLANO.md, as entidades JPA e os repositories. Adicione uma migration de seed com 1 admin, 3 profissionais, 5 serviços e jornadas de segunda a sábado.
```

**Fase 2 — Autenticação**
```
Implemente Spring Security com JWT (access + refresh), registro, login, perfis ADMIN e CLIENTE e o tratamento global de erros. Escreva testes de integração para os fluxos de auth.
```

**Fase 3 — Regras de negócio**
```
Implemente os CRUDs de serviços e profissionais, o cálculo de disponibilidade (respeitando jornada, duração do serviço e agendamentos existentes) e o fluxo de agendamento com regra de cancelamento até 2h antes. Cubra disponibilidade e conflitos com testes unitários e com Testcontainers. Documente tudo no Swagger.
```

**Fase 4 — Front-end do cliente**
```
No /frontend, crie: tela de login/cadastro, lista de serviços, fluxo de agendamento em etapas (serviço → profissional → data → horário → confirmar) e "Meus agendamentos" com cancelamento. Use Tailwind, layout responsivo, estados de carregamento e erro, e rotas protegidas.
```

**Fase 5 — Painel admin**
```
Crie o painel admin: dashboard (agendamentos do dia, faturamento do mês, top serviços com gráfico), CRUD de serviços e profissionais e lista de agendamentos com filtros e paginação.
```

**Fase 6 — Entrega**
```
Revise segurança (CORS, validações, senhas), rode todos os testes, deixe o docker-compose subindo o sistema inteiro com um comando, e escreva um README com prints, diagrama do banco, credenciais de demonstração e instruções. Sugira opções gratuitas de deploy (ex.: Render/Railway para back e banco, Vercel para front) com o passo a passo.
```

## Critérios de pronto

- [ ] `docker compose up` sobe banco, API e front sem passos manuais
- [ ] Impossível criar dois agendamentos no mesmo horário para o mesmo profissional (testado)
- [ ] Swagger acessível e completo
- [ ] Testes passando, com cobertura das regras de disponibilidade e cancelamento
- [ ] Deploy online com usuário de demonstração

## Como cadastrar no Workana

- **Título:** Sistema Web de Agendamentos — React + Spring Boot
- **Habilidades:** Java, React.js, JavaScript
- **Descrição:** Sistema full stack de agendamento online para salões e clínicas. Área do cliente para marcar e cancelar horários, painel administrativo com dashboard de faturamento e cálculo automático de horários livres sem conflitos. API REST em Java/Spring Boot com autenticação JWT, PostgreSQL, testes automatizados e Docker.
- **Imagens:** fluxo de agendamento, dashboard admin, Swagger
- **Link:** deploy + repositório no GitHub
