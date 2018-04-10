/*
Copyright 2018 Spotify AB. All rights reserved.

The contents of this file are licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cmd

import (
	"errors"
	"fmt"
	"os"
	"strings"

	"github.com/spf13/cobra"
	"github.com/spotify/protoman/cli/protoman"
)

var rootCmd = &cobra.Command{
	Use:   "protoman",
	Short: "Protoman is a fantastic way of managing your protos",
}

func init() {
	rootCmd.AddCommand(versionCmd, publishCmd, getCmd, updateCmd, addCmd)
	publishCmd.PersistentFlags().StringP("server", "s", "", "Protoman server address")
	publishCmd.PersistentFlags().BoolP("force", "f", false, "Force publish of protos")
	publishCmd.PersistentFlags().BoolP("dry-run", "d", false, "Validation only publish")
	getCmd.PersistentFlags().StringP("server", "s", "", "Protoman server address")
	updateCmd.PersistentFlags().StringP("server", "s", "", "Protoman server address")
	getCmd.PersistentFlags().StringP("proto-dir", "p", "", "Root directory where protos will be stored")
	addCmd.PersistentFlags().StringP("path", "p", "", "Path to package")
}

func exitOnErr(err error) {
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print the version number of protoman",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("Protoman version 0.0.1")
	},
}

var addCmd = &cobra.Command{
	Use:   "add [package name]",
	Short: "Add package to .protoman",
	Args: func(cmd *cobra.Command, args []string) error {
		if len(args) < 1 {
			return errors.New("package name required")
		}
		if cmd.Flag("path").Value.String() == "" {
			return errors.New("You must specify package --path")
		}
		f, err := os.Stat(cmd.Flag("path").Value.String())
		if err != nil {
			return err
		}
		if !f.IsDir() {
			return errors.New("path must be a directory")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		exitOnErr(protoman.Add(args[0], cmd.Flag("path").Value.String()))
	},
}

var publishCmd = &cobra.Command{
	Use:   "publish [protos]",
	Short: "Publish proto defintion file(s) to a protoman registry.",
	Long: `
	Publish proto defintion file(s) to a protoman registry.
	Providing no arguments will upload local packages defined in .protoman`,
	Args: func(cmd *cobra.Command, args []string) error {
		if cmd.Flag("server").Value.String() == "" {
			return errors.New("--server must be set to the protoman registry")
		}
		if len(args) == 0 {
			return nil
		}
		for _, proto := range args {
			if !strings.HasSuffix(proto, ".proto") {
				return errors.New("must be .proto files")
			}
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		client, err := protoman.NewRegistryClient(cmd.Flag("server").Value.String())
		exitOnErr(err)
		dryRun, _ := cmd.Flags().GetBool("dry-run")
		force, _ := cmd.Flags().GetBool("force")
		exitOnErr(protoman.Publish(
			args,
			client,
			dryRun,
			force))
	},
}

var updateCmd = &cobra.Command{
	Use:   "update",
	Short: "Update all dependencies",
	Args: func(cmd *cobra.Command, args []string) error {
		if cmd.Flag("server").Value.String() == "" {
			return errors.New("--server must be set to the protoman registry")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		client, err := protoman.NewRegistryClient(cmd.Flag("server").Value.String())
		exitOnErr(err)
		exitOnErr(protoman.Update(client))
	},
}
var getCmd = &cobra.Command{
	Use:   "get [package names]",
	Short: "Get package",
	Args: func(cmd *cobra.Command, args []string) error {
		if cmd.Flag("server").Value.String() == "" {
			return errors.New("--server must be set to the protoman registry")
		}
		if cmd.Flag("proto-dir").Value.String() == "" {
			return errors.New("--proto-dir must be specified")
		}
		if len(args) == 0 {
			return errors.New("at least one package name must be specified")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		path := cmd.Flag("proto-dir").Value.String()
		client, err := protoman.NewRegistryClient(cmd.Flag("server").Value.String())
		exitOnErr(err)
		exitOnErr(protoman.Get(client, path, args))
	},
}

// Execute CLI
func Execute() {
	exitOnErr(rootCmd.Execute())
}
