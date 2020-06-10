# ClosedDiversity

Description :
Les jeux de données sont stockées dans le fichier data/ en fichier compressé zip. Tous les jeux de données proviennent du dépôt CP4IM "Tias Guns".
Pour que le projet compile, utiliser Maven et le fichier pom.xml qui va importer toutes les librairies nécessaires (Choco 4 en particulier). 
Sinon, il faut télécharger et ajouter Choco 4 au projet et l'ajouter en "external jar" au projet. (téléchargement: https://github.com/chocoteam/choco-solver/releases/tag/4.0.2).


Options de lancement :

seuil max de la diversité [0,1] pourcentage.
seuil min de la fréquence [0,1] pourcentage.
exemple : mushroom 0.2 0.3 


############################################################
diversity topk
#################
Arguments :
	arg1 : path to dataset
	arg2 : frequency threshold
	arg3 : path to results file
	arg4 : path to analyze file
	arg6 : agregation function --> MAX, AVG, MIN (optional, by default MAX)
  
  
