FROM hseeberger/scala-sbt

# Install deps
RUN apt-key update
RUN apt-get update
RUN apt-get -y install ruby ruby-dev make gcc
RUN gem install bundler

# Install gems
COPY Gemfile /tmp/Gemfile
ENV BUNDLE_GEMFILE=/tmp/Gemfile
RUN bundle install --system
RUN rm /tmp/Gemfile

RUN mkdir /frontend
WORKDIR /frontend

EXPOSE 9000
