{
  description = "Flake to manage clojure workspace";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/master";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [
            clojure
            jdk
            leiningen
          ];
          shellHook = ''
            export JAVA_HOME=${pkgs.jdk}
            PATH="${pkgs.jdk}/bin:$PATH"
          '';
        };
      });
}
