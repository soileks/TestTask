package org;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


import java.util.Map;


public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private User currentUser;
    VoteCreationState voteCreationState;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        msg = msg.trim();

        if (voteCreationState != null) {
            if (voteCreationState.isVoting()) {
                // Обработка выбора пользователя для голосования
                handleVoteChoice(ctx, msg);
            } else {
                // Обработка ответов для создания голосования
                handleVoteCreation(ctx, msg);
            }
            return;
        }

        // Обработка обычных команд
        String[] parts = msg.split("\\s+");
        if (parts.length == 0) {
            ctx.writeAndFlush("Unknown command\n");
            return;
        }

        String command = parts[0];
        switch (command) {
            case "login":
                if (parts.length < 2) {
                    ctx.writeAndFlush("Invalid login format. Use: login -u=username\n");
                } else {
                    handleLogin(ctx, parts[1]);
                }
                break;
            case "create":
                if (parts.length < 3) {
                    ctx.writeAndFlush("Invalid create command\n");
                } else {
                    handleCreate(ctx, parts);
                }
                break;
            case "view":
                handleView(ctx, parts);
                break;
            case "vote":
                if (parts.length < 3) {
                    ctx.writeAndFlush("Invalid vote command\n");
                } else {
                    handleVote(ctx, parts);
                }
                break;
            case "delete":
                if (parts.length < 3) {
                    ctx.writeAndFlush("Invalid delete command\n");
                } else {
                    handleDelete(ctx, parts);
                }
                break;
            case "load":
                if (parts.length < 2) {
                    ctx.writeAndFlush("Invalid load command. Use: load <filename>\n");
                } else {
                    handleLoad(ctx, parts[1]);
                }
                break;
            case "save":
                if (parts.length < 2) {
                    ctx.writeAndFlush("Invalid save command. Use: save <filename>\n");
                } else {
                    handleSave(ctx, parts[1]);
                }
                break;
            case "exit":
                ctx.close();
                break;
            default:
                ctx.writeAndFlush("Unknown command\n");
        }
    }

    private void handleLoad(ChannelHandlerContext ctx, String filename) {
        try {
            VotingServer.loadFromFile(filename);
            ctx.writeAndFlush("Data loaded from " + filename + "\n");
        } catch (Exception e) {
            ctx.writeAndFlush("Failed to load data: " + e.getMessage() + "\n");
        }
    }

    private void handleSave(ChannelHandlerContext ctx, String filename) {
        try {
            VotingServer.saveToFile(filename);
            ctx.writeAndFlush("Data saved to " + filename + "\n");
        } catch (Exception e) {
            ctx.writeAndFlush("Failed to save data: " + e.getMessage() + "\n");
        }
    }

    private void handleVoteCreation(ChannelHandlerContext ctx, String msg) {
        if (voteCreationState.getVoteName() == null) {
            // Запрос имени голосования
            voteCreationState.setVoteName(msg);
            ctx.writeAndFlush("Enter vote description: ");
        } else if (voteCreationState.getVoteDescription() == null) {
            // Запрос описания голосования
            voteCreationState.setVoteDescription(msg);
            ctx.writeAndFlush("Enter number of options: ");
        } else if (voteCreationState.getOptionsCount() == 0) {
            // Запрос количества вариантов
            try {
                int optionsCount = Integer.parseInt(msg);
                if (optionsCount <= 0) {
                    ctx.writeAndFlush("Number of options must be greater than 0\n");
                    return;
                }
                voteCreationState.setOptionsCount(optionsCount);
                ctx.writeAndFlush("Enter option 1: ");
            } catch (NumberFormatException e) {
                ctx.writeAndFlush("Invalid number of options\n");
            }
        } else {
            // Запрос вариантов ответа
            voteCreationState.addOption(msg);
            if (voteCreationState.getOptions().size() < voteCreationState.getOptionsCount()) {
                ctx.writeAndFlush("Enter option " + (voteCreationState.getOptions().size() + 1) + ": ");
            } else {
                // Все данные получены, создаем голосование
                Topic topic = VotingServer.getTopics().get(voteCreationState.getTopicName());
                Vote vote = new Vote(
                        voteCreationState.getVoteName(),
                        voteCreationState.getVoteDescription(),
                        voteCreationState.getOptions(),
                        currentUser.getUsername()
                );
                topic.addVote(vote);
                ctx.writeAndFlush("Vote created: " + voteCreationState.getVoteName() + "\n");
                voteCreationState = null; // Сбрасываем состояние
            }
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, String arg) {
        if (!arg.startsWith("-u=")) {
            ctx.writeAndFlush("Invalid login format. Use: login -u=username\n");
            return;
        }
        String username = arg.substring(3); // Извлекаем имя пользователя после "-u="
        currentUser = new User(username);
        VotingServer.getUsers().put(username, currentUser);
        ctx.writeAndFlush("Logged in as " + username + "\n");
    }

    private void handleCreate(ChannelHandlerContext ctx, String[] parts) {
        if (currentUser == null) {
            ctx.writeAndFlush("You need to login first\n");
            return;
        }

        if (parts.length < 2) {
            ctx.writeAndFlush("Invalid create command\n");
            return;
        }

        if (parts[1].equals("topic")) {
            // Обработка команды create topic
            if (parts.length < 3) {
                ctx.writeAndFlush("Invalid create topic command. Use: create topic -n=<topic>\n");
                return;
            }
            String topicName = parts[2].split("=")[1];
            if (VotingServer.getTopics().containsKey(topicName)) {
                ctx.writeAndFlush("Topic already exists: " + topicName + "\n");
            } else {
                VotingServer.getTopics().put(topicName, new Topic(topicName));
                ctx.writeAndFlush("Topic created: " + topicName + "\n");
            }
        } else if (parts[1].equals("vote")) {
            // Обработка команды create vote
            if (parts.length < 3) {
                ctx.writeAndFlush("Invalid create vote command. Use: create vote -t=<topic>\n");
                return;
            }
            String topicName = parts[2].split("=")[1];
            Topic topic = VotingServer.getTopics().get(topicName);
            if (topic == null) {
                ctx.writeAndFlush("Topic not found: " + topicName + "\n");
                return;
            }

            // Инициируем процесс создания голосования
            voteCreationState = new VoteCreationState();
            voteCreationState.setTopicName(topicName);
            ctx.writeAndFlush("Enter vote name: ");
        } else {
            ctx.writeAndFlush("Unknown create command\n");
        }
    }
    private void handleVoteChoice(ChannelHandlerContext ctx, String msg) {
        Topic topic = VotingServer.getTopics().get(voteCreationState.getTopicName());
        Vote vote = topic.getVotes().stream()
                .filter(v -> v.getName().equals(voteCreationState.getVoteName()))
                .findFirst()
                .orElse(null);

        if (vote == null) {
            ctx.writeAndFlush("Vote not found\n");
            voteCreationState = null;
            return;
        }

        try {
            int choice = Integer.parseInt(msg) - 1;
            if (choice < 0 || choice >= vote.getOptions().size()) {
                ctx.writeAndFlush("Invalid choice\n");
            } else {
                // Голосуем
                vote.vote(vote.getOptions().get(choice));
                ctx.writeAndFlush("Vote recorded\n");
            }
        } catch (NumberFormatException e) {
            ctx.writeAndFlush("Invalid choice\n");
        }

        voteCreationState = null; // Сбрасываем состояние
    }

    private void handleView(ChannelHandlerContext ctx, String[] parts) {
        if (parts.length == 1) {
            // Показать все разделы
            if (VotingServer.getTopics().isEmpty()) {
                ctx.writeAndFlush("No topics found\n");
            } else {
                StringBuilder response = new StringBuilder("Topics:\n");
                for (Map.Entry<String, Topic> entry : VotingServer.getTopics().entrySet()) {
                    Topic topic = entry.getValue();
                    response.append(topic.getName())
                            .append(" (votes in topic=")
                            .append(topic.getVotes().size())
                            .append(")\n");
                }
                ctx.writeAndFlush(response.toString());
            }
        } else if (parts.length == 2 && parts[1].startsWith("-t=")) {
            // Показать голосования в конкретном разделе
            String topicName = parts[1].split("=")[1];
            Topic topic = VotingServer.getTopics().get(topicName);
            if (topic == null) {
                ctx.writeAndFlush("Topic not found: " + topicName + "\n");
            } else {
                StringBuilder response = new StringBuilder("Votes in topic " + topicName + ":\n");
                for (Vote vote : topic.getVotes()) {
                    response.append(vote.getName())
                            .append(" (created by ")
                            .append(vote.getCreator())
                            .append(")\n");
                }
                ctx.writeAndFlush(response.toString());
            }
        } else if (parts.length == 3 && parts[1].startsWith("-t=") && parts[2].startsWith("-v=")) {
            // Показать информацию о конкретном голосовании
            String topicName = parts[1].split("=")[1];
            String voteName = parts[2].split("=")[1];

            Topic topic = VotingServer.getTopics().get(topicName);
            if (topic == null) {
                ctx.writeAndFlush("Topic not found: " + topicName + "\n");
                return;
            }

            Vote vote = topic.getVotes().stream()
                    .filter(v -> v.getName().equals(voteName))
                    .findFirst()
                    .orElse(null);

            if (vote == null) {
                ctx.writeAndFlush("Vote not found: " + voteName + "\n");
                return;
            }

            // Выводим информацию о голосовании
            StringBuilder response = new StringBuilder("Vote: " + vote.getName() + "\n");
            response.append("Description: ").append(vote.getDescription()).append("\n");
            response.append("Results:\n");
            for (Map.Entry<String, Integer> entry : vote.getResults().entrySet()) {
                response.append(entry.getKey()).append(": ").append(entry.getValue()).append(" votes\n");
            }
            ctx.writeAndFlush(response.toString());
        } else {
            ctx.writeAndFlush("Invalid view command\n");
        }
    }

    private void handleVote(ChannelHandlerContext ctx, String[] parts) {
        if (currentUser == null) {
            ctx.writeAndFlush("You need to login first\n");
            return;
        }

        if (parts.length < 3) {
            ctx.writeAndFlush("Invalid vote command. Use: vote -t=<topic> -v=<vote>\n");
            return;
        }

        String topicName = parts[1].split("=")[1];
        String voteName = parts[2].split("=")[1];

        Topic topic = VotingServer.getTopics().get(topicName);
        if (topic == null) {
            ctx.writeAndFlush("Topic not found: " + topicName + "\n");
            return;
        }

        Vote vote = topic.getVotes().stream()
                .filter(v -> v.getName().equals(voteName))
                .findFirst()
                .orElse(null);

        if (vote == null) {
            ctx.writeAndFlush("Vote not found: " + voteName + "\n");
            return;
        }

        // Устанавливаем состояние для голосования
        voteCreationState = new VoteCreationState();
        voteCreationState.setTopicName(topicName);
        voteCreationState.setVoteName(voteName);
        voteCreationState.setVoting(true); // Указываем, что это состояние голосования

        // Показываем варианты ответа
        StringBuilder options = new StringBuilder("Options:\n");
        for (int i = 0; i < vote.getOptions().size(); i++) {
            options.append(i + 1).append(". ").append(vote.getOptions().get(i)).append("\n");
        }
        ctx.writeAndFlush(options.toString());

        // Запрашиваем выбор пользователя
        ctx.writeAndFlush("Enter your choice (1-" + vote.getOptions().size() + "): ");
    }

    private void handleDelete(ChannelHandlerContext ctx, String[] parts) {
        if (currentUser == null) {
            ctx.writeAndFlush("You need to login first\n");
            return;
        }

        String topicName = parts[1].split("=")[1];
        String voteName = parts[2].split("=")[1];

        Topic topic = VotingServer.getTopics().get(topicName);
        if (topic == null) {
            ctx.writeAndFlush("Topic not found: " + topicName + "\n");
            return;
        }

        Vote vote = topic.getVotes().stream()
                .filter(v -> v.getName().equals(voteName))
                .findFirst()
                .orElse(null);

        if (vote == null) {
            ctx.writeAndFlush("Vote not found: " + voteName + "\n");
            return;
        }

        // Проверяем, что текущий пользователь — создатель голосования
        if (!vote.getCreator().equals(currentUser.getUsername())) {
            ctx.writeAndFlush("You are not the creator of this vote\n");
            return;
        }

        // Удаляем голосование
        topic.removeVote(vote);
        ctx.writeAndFlush("Vote deleted: " + voteName + "\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}