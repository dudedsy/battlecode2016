all:
	ant run

clean:
	ant clean

jar:
	rm -v team028-submission.jar
	ant -Dteam=team028 jar
